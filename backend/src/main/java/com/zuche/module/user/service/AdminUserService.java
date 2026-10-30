package com.zuche.module.user.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zuche.common.BusinessException;
import com.zuche.common.PageResult;
import com.zuche.common.ResultCode;
import com.zuche.module.credit.service.CreditService;
import com.zuche.module.credit.vo.CreditChangeVO;
import com.zuche.module.user.dto.AdminUserQueryDTO;
import com.zuche.module.user.entity.SysUser;
import com.zuche.module.user.mapper.AdminUserMapper;
import com.zuche.module.user.mapper.SysUserMapper;
import com.zuche.module.user.vo.AdminUserStatsVO;
import com.zuche.module.user.vo.AdminUserVO;
import com.zuche.security.AccountStatusGuard;
import com.zuche.security.RoleCode;
import com.zuche.security.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * 平台用户管理
 *
 * <p>三条自我保护的规矩，缺一条就可能把自己锁在系统外面：</p>
 * <ol>
 *   <li>不能冻结自己</li>
 *   <li>不能冻结最后一个正常的系统管理员</li>
 *   <li>调分必须写原因，且走 CreditService（日志可回溯）</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private static final Map<String, String> LEVEL_LABELS = new HashMap<>();
    private static final Map<String, String> LEVEL_COLORS = new HashMap<>();

    static {
        LEVEL_LABELS.put("EXCELLENT", "优秀");
        LEVEL_LABELS.put("GOOD", "良好");
        LEVEL_LABELS.put("NORMAL", "一般");
        LEVEL_LABELS.put("WATCH", "待观察");

        LEVEL_COLORS.put("EXCELLENT", "#67C23A");
        LEVEL_COLORS.put("GOOD", "#409EFF");
        LEVEL_COLORS.put("NORMAL", "#E6A23C");
        LEVEL_COLORS.put("WATCH", "#F56C6C");
    }

    private final AdminUserMapper adminUserMapper;
    private final SysUserMapper userMapper;
    private final CreditService creditService;
    private final AccountStatusGuard accountStatusGuard;

    /* ==================== 查询 ==================== */

    public PageResult<AdminUserVO> page(AdminUserQueryDTO query) {
        Page<AdminUserVO> page = new Page<>(query.getCurrent(), query.getPageSize());
        IPage<AdminUserVO> result = adminUserMapper.selectUserPage(page, query);
        result.getRecords().forEach(this::decorate);
        return PageResult.of(result);
    }

    public AdminUserStatsVO stats() {
        AdminUserStatsVO vo = adminUserMapper.selectStats();
        if (vo == null) {
            vo = new AdminUserStatsVO();
        }
        vo.setTotal(nz(vo.getTotal()));
        vo.setNormal(nz(vo.getNormal()));
        vo.setFrozen(nz(vo.getFrozen()));
        vo.setCustomers(nz(vo.getCustomers()));
        vo.setStoreAdmins(nz(vo.getStoreAdmins()));
        vo.setMonthNew(nz(vo.getMonthNew()));
        return vo;
    }

    public AdminUserVO detail(Long id) {
        AdminUserVO vo = adminUserMapper.selectUserById(id);
        if (vo == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }
        return decorate(vo);
    }

    /* ==================== 冻结 / 解冻 ==================== */

    /**
     * 冻结或解冻账号
     *
     * <p>冻结不删除任何数据，只改 status：订单、信用日志、评价都还在，
     * 解冻后一切照旧。这是「可逆」和「不可逆」操作的分界线。</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public AdminUserVO toggleStatus(Long id, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "状态只能是 0（冻结）或 1（正常）");
        }
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }

        Long operatorId = UserContext.requireUserId();
        if (operatorId.equals(id) && status == 0) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR,
                    "不能冻结当前登录账号，否则会把自己锁在系统外面");
        }

        String roleCode = userMapper.selectRoleCode(id);
        if (status == 0 && RoleCode.SYS_ADMIN.equals(roleCode)) {
            int activeAdmins = adminUserMapper.countActiveByRole(RoleCode.SYS_ADMIN);
            if (activeAdmins <= 1) {
                throw new BusinessException(ResultCode.BUSINESS_ERROR,
                        "这是最后一个可用的系统管理员，冻结后将无人能管理系统");
            }
        }

        if (Integer.valueOf(status).equals(user.getStatus())) {
            return decorate(adminUserMapper.selectUserById(id));
        }

        SysUser update = new SysUser();
        update.setId(id);
        update.setStatus(status);
        userMapper.updateById(update);
        // 让冻结立刻生效，不等 60 秒缓存过期
        accountStatusGuard.evict(id);

        log.info("账号状态变更: userId={}, {} -> {}", id, user.getStatus(), status);
        return decorate(adminUserMapper.selectUserById(id));
    }

    /* ==================== 信用调整 ==================== */

    /**
     * 人工调分
     *
     * <p>不允许在这里直接 update 分数——必须经过 CreditService，
     * 它会在同一事务里写 credit_log、回写分数、重算等级，
     * 保证「credit_score == 100 + Σ credit_log.change」这条恒等式不被破坏。</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public CreditChangeVO adjustCredit(Long userId, Integer scoreChange, String reason) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }
        if (scoreChange == null || scoreChange == 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "调整分值不能为 0");
        }
        if (!StringUtils.hasText(reason)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "调整原因不能为空");
        }
        Long operatorId = UserContext.requireUserId();
        CreditChangeVO vo = creditService.adjustByAdmin(userId, scoreChange, reason.trim(), operatorId);
        log.info("人工调分: userId={}, {}{} -> {}, 原因={}",
                userId, scoreChange > 0 ? "+" : "", scoreChange, vo.getScoreAfter(), reason);
        return vo;
    }

    /* ==================== 内部 ==================== */

    private AdminUserVO decorate(AdminUserVO vo) {
        if (vo == null) {
            return null;
        }
        String level = vo.getCreditLevel();
        vo.setLevelLabel(label(LEVEL_LABELS, level));
        vo.setLevelColor(level == null ? "#909399" : LEVEL_COLORS.getOrDefault(level, "#909399"));
        vo.setStatusLabel(Integer.valueOf(1).equals(vo.getStatus()) ? "正常" : "已冻结");
        vo.setOrderCount(nz(vo.getOrderCount()));
        vo.setFinishedCount(nz(vo.getFinishedCount()));
        vo.setActiveCount(nz(vo.getActiveCount()));
        vo.setTotalSpend(vo.getTotalSpend() == null ? ZERO : vo.getTotalSpend());
        return vo;
    }

    /** 空安全的字典翻译：HashMap 允许 null key，但仍然显式挡一下，语义更清楚 */
    private static String label(Map<String, String> dict, String code) {
        if (code == null) {
            return null;
        }
        return dict.getOrDefault(code, code);
    }

    private static int nz(Integer value) {
        return value == null ? 0 : value;
    }
}
