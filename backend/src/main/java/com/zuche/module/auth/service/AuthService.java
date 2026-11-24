package com.zuche.module.auth.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zuche.common.BusinessException;
import com.zuche.common.ResultCode;
import com.zuche.module.auth.dto.LoginDTO;
import com.zuche.module.auth.dto.PasswordChangeDTO;
import com.zuche.module.auth.dto.ProfileUpdateDTO;
import com.zuche.module.auth.dto.RegisterDTO;
import com.zuche.module.auth.vo.LoginVO;
import com.zuche.module.user.entity.SysUser;
import com.zuche.module.user.mapper.SysUserMapper;
import com.zuche.security.JwtUtil;
import com.zuche.security.RoleCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * 认证服务：登录、注册、当前用户
 *
 * <p>密码使用 BCrypt（只用 spring-security-crypto，不引入完整 Spring Security）。
 * 注册只开放普通用户；信用分固定 100（良好档），后续由租赁行为动态调整。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String DEFAULT_AVATAR = "/images/avatar/default.svg";

    private final SysUserMapper userMapper;
    private final JwtUtil jwtUtil;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public LoginVO login(LoginDTO dto) {
        SysUser user = userMapper.selectOne(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getUsername, dto.getUsername().trim()));

        // 用户不存在与密码错误返回同一提示，避免被撞库探测
        if (user == null || !encoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.LOGIN_FAILED);
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException(ResultCode.ACCOUNT_DISABLED);
        }

        String roleCode = roleOf(user.getId());
        String token = jwtUtil.createToken(user.getId(), user.getUsername(), roleCode, user.getStoreId());

        // 记录最近登录时间：后台「用户管理」要靠它判断账号是否还活跃。
        // 放在签发 token 之后、返回之前，登录失败时不会留下脏时间。
        SysUser touch = new SysUser();
        touch.setId(user.getId());
        touch.setLastLoginTime(LocalDateTime.now());
        userMapper.updateById(touch);

        log.info("用户登录成功: {} ({})", user.getUsername(), roleCode);
        return buildLoginVO(user, roleCode, token);
    }

    /**
     * 注册普通用户
     *
     * <p>事务内做三件事：写 sys_user → 绑定 USER 角色 → 签发 token（注册即登录）。
     * 用户名/手机号先查重，避免直接撞唯一索引抛 SQL 异常给前端。</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public LoginVO register(RegisterDTO dto) {
        String username = dto.getUsername().trim();
        String phone = dto.getPhone().trim();

        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            throw new BusinessException("两次输入的密码不一致");
        }
        if (userMapper.countByUsername(username) > 0) {
            throw new BusinessException("用户名已被占用，请更换");
        }
        if (userMapper.countByPhone(phone) > 0) {
            throw new BusinessException("该手机号已注册，请直接登录");
        }

        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPassword(encoder.encode(dto.getPassword()));
        user.setRealName(dto.getRealName().trim());
        user.setPhone(phone);
        user.setEmail(StringUtils.hasText(dto.getEmail()) ? dto.getEmail().trim() : null);
        user.setAvatar(DEFAULT_AVATAR);
        user.setCreditScore(100);
        user.setCreditLevel("GOOD");
        user.setStatus(1);
        userMapper.insert(user);

        Long roleId = userMapper.selectRoleIdByCode(RoleCode.USER);
        if (roleId == null) {
            throw new BusinessException("系统角色数据缺失，请联系管理员");
        }
        userMapper.insertUserRole(user.getId(), roleId);

        String token = jwtUtil.createToken(user.getId(), user.getUsername(), RoleCode.USER, null);
        log.info("新用户注册成功: {} (id={}, 初始信用分=100)", username, user.getId());
        return buildLoginVO(user, RoleCode.USER, token);
    }

    /**
     * 当前登录人信息（每次查库，保证信用分实时）
     */
    public LoginVO currentUser(Long userId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "账号不存在");
        }
        return buildLoginVO(user, roleOf(user.getId()), null);
    }

    /**
     * 修改个人资料（姓名 / 手机号 / 邮箱）
     *
     * <p>手机号做了唯一校验：登录虽用用户名，但手机号在注册时就是唯一的，
     * 这里不校验会出现两个账号同手机号，后续做短信找回密码就分不清人。</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public LoginVO updateProfile(Long userId, ProfileUpdateDTO dto) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "账号不存在");
        }
        String phone = dto.getPhone().trim();
        if (!phone.equals(user.getPhone()) && userMapper.countByPhone(phone) > 0) {
            throw new BusinessException("该手机号已被其他账号使用");
        }

        SysUser update = new SysUser();
        update.setId(userId);
        update.setRealName(dto.getRealName().trim());
        update.setPhone(phone);
        update.setEmail(StringUtils.hasText(dto.getEmail()) ? dto.getEmail().trim() : null);
        userMapper.updateById(update);

        log.info("用户修改资料: id={}", userId);
        return currentUser(userId);
    }

    /**
     * 修改密码：先校验原密码，再确认两次新密码一致，且新旧不能相同
     */
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(Long userId, PasswordChangeDTO dto) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "账号不存在");
        }
        if (!encoder.matches(dto.getOldPassword(), user.getPassword())) {
            throw new BusinessException("原密码不正确");
        }
        if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
            throw new BusinessException("两次输入的新密码不一致");
        }
        if (dto.getNewPassword().equals(dto.getOldPassword())) {
            throw new BusinessException("新密码不能与原密码相同");
        }

        SysUser update = new SysUser();
        update.setId(userId);
        update.setPassword(encoder.encode(dto.getNewPassword()));
        userMapper.updateById(update);

        log.info("用户修改密码: id={}", userId);
    }

    private String roleOf(Long userId) {
        String roleCode = userMapper.selectRoleCode(userId);
        return roleCode == null ? RoleCode.USER : roleCode;
    }

    private LoginVO buildLoginVO(SysUser user, String roleCode, String token) {
        return LoginVO.builder()
                .token(token)
                .userId(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .phone(user.getPhone())
                .email(user.getEmail())
                .avatar(user.getAvatar())
                .roleCode(roleCode)
                .creditScore(user.getCreditScore())
                .creditLevel(user.getCreditLevel())
                .storeId(user.getStoreId())
                .build();
    }
}
