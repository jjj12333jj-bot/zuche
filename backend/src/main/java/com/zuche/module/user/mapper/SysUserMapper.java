package com.zuche.module.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zuche.module.risk.vo.AdminUserCreditVO;
import com.zuche.module.user.entity.SysUser;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 用户 Mapper
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    /**
     * 取用户角色码（本系统一个用户只有一个角色）
     */
    @Select("""
            SELECT r.role_code
              FROM sys_role r
              JOIN sys_user_role ur ON ur.role_id = r.id
             WHERE ur.user_id = #{userId}
             LIMIT 1
            """)
    String selectRoleCode(@Param("userId") Long userId);

    /** 按角色码取角色 ID（注册时绑定 USER 角色） */
    @Select("SELECT id FROM sys_role WHERE role_code = #{roleCode} AND deleted = 0 LIMIT 1")
    Long selectRoleIdByCode(@Param("roleCode") String roleCode);

    /** 绑定用户角色 */
    @Insert("INSERT INTO sys_user_role (user_id, role_id) VALUES (#{userId}, #{roleId})")
    int insertUserRole(@Param("userId") Long userId, @Param("roleId") Long roleId);

    /** 用户名是否已存在（含已逻辑删除，与唯一索引口径一致） */
    @Select("SELECT COUNT(*) FROM sys_user WHERE username = #{username}")
    long countByUsername(@Param("username") String username);

    /** 手机号是否已存在（含已逻辑删除，与唯一索引口径一致） */
    @Select("SELECT COUNT(*) FROM sys_user WHERE phone = #{phone}")
    long countByPhone(@Param("phone") String phone);

    /**
     * 行锁读取用户：信用分读改写必须锁行，否则并发下两个人同时读到 100 分，
     * 各加 5 分，最后只落成 105（经典丢失更新）。
     */
    @Select("SELECT * FROM sys_user WHERE id = #{id} AND deleted = 0 FOR UPDATE")
    SysUser selectByIdForUpdate(@Param("id") Long id);

    /** 回写信用分与等级（只允许 CreditService 调用，业务模块禁止直接改） */
    @Update("UPDATE sys_user SET credit_score = #{score}, credit_level = #{level}, "
            + "update_time = NOW() WHERE id = #{id} AND deleted = 0")
    int updateCredit(@Param("id") Long id,
                     @Param("score") Integer score,
                     @Param("level") String level);

    /**
     * 按当前等级区间全量重算 credit_level
     *
     * <p>后台把某档下限从 120 调到 125 后，120~124 分的用户必须当场掉档，
     * 否则页面上等级还是旧的，押金仍按旧系数算。只更新真正变化的行。</p>
     *
     * @return 实际变更的用户数
     */
    @Update("""
            UPDATE sys_user u
              JOIN credit_rule r
                ON r.rule_type = 'LEVEL'
               AND r.status = 1
               AND r.deleted = 0
               AND u.credit_score BETWEEN r.min_score AND r.max_score
               SET u.credit_level = r.rule_code,
                   u.update_time = NOW()
             WHERE u.deleted = 0
               AND u.credit_level <> r.rule_code
            """)
    int recalcLevels();

    /**
     * 后台用户信用分页
     *
     * <p>只查前台用户（角色 USER）：管理员账号不参与租赁，列进来只会干扰调分操作。
     * 风险状态用相关子查询取"当前生效的最高强度"，避免把历史已解除的记录带出来。</p>
     */
    IPage<AdminUserCreditVO> selectAdminUserPage(Page<AdminUserCreditVO> page,
                                                 @Param("keyword") String keyword,
                                                 @Param("level") String level,
                                                 @Param("riskType") String riskType);
}
