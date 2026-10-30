package com.zuche.module.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zuche.module.system.entity.SysMenu;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 菜单 Mapper
 */
@Mapper
public interface SysMenuMapper extends BaseMapper<SysMenu> {

    /**
     * 按用户查询可见菜单（目录 + 菜单，不含按钮 F）
     *
     * <p>一个用户在本系统中只有一个角色，但仍用 DISTINCT 防止脏授权数据导致重复。</p>
     */
    @Select("""
            SELECT DISTINCT m.id, m.parent_id, m.menu_name, m.menu_type, m.path,
                            m.component, m.perms, m.icon, m.sort_no, m.visible, m.status
              FROM sys_menu m
              JOIN sys_role_menu rm ON rm.menu_id = m.id
              JOIN sys_user_role ur ON ur.role_id = rm.role_id
             WHERE ur.user_id = #{userId}
               AND m.menu_type IN ('M', 'C')
               AND m.status = 1
               AND m.visible = 1
               AND m.deleted = 0
             ORDER BY m.sort_no, m.id
            """)
    List<SysMenu> selectMenusByUserId(@Param("userId") Long userId);
}
