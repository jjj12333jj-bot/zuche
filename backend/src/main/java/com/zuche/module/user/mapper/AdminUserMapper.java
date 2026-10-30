package com.zuche.module.user.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zuche.module.user.dto.AdminUserQueryDTO;
import com.zuche.module.user.vo.AdminUserStatsVO;
import com.zuche.module.user.vo.AdminUserVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 平台用户查询
 *
 * <p>不复用 {@code SysUserMapper.selectAdminUserPage}：那份只查普通用户且带风控状态，
 * 是给信用页用的。这份要含管理员、角色、消费额，条件完全不同。</p>
 */
@Mapper
public interface AdminUserMapper {

    IPage<AdminUserVO> selectUserPage(Page<AdminUserVO> page, @Param("q") AdminUserQueryDTO query);

    AdminUserVO selectUserById(@Param("id") Long id);

    AdminUserStatsVO selectStats();

    /** 统计某角色下仍正常的账号数，用于「不能冻结最后一个管理员」的拦截 */
    int countActiveByRole(@Param("roleCode") String roleCode);
}
