package com.zuche.module.system.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 菜单树节点（供前端动态渲染侧边栏）
 */
@Data
@Schema(description = "菜单树节点")
public class MenuVO {

    private Long id;

    private Long parentId;

    @Schema(description = "菜单名称")
    private String menuName;

    @Schema(description = "M 目录 / C 菜单")
    private String menuType;

    @Schema(description = "前端路由地址")
    private String path;

    @Schema(description = "前端组件路径，如 portal/VehicleList")
    private String component;

    @Schema(description = "权限标识，如 vehicle:list")
    private String perms;

    @Schema(description = "Element Plus 图标名")
    private String icon;

    private Integer sortNo;

    private List<MenuVO> children = new ArrayList<>();
}
