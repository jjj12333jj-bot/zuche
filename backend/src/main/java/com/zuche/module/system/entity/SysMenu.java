package com.zuche.module.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 菜单/权限实体（对应 sys_menu）
 */
@Data
@TableName("sys_menu")
@Schema(description = "菜单")
public class SysMenu {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long parentId;

    private String menuName;

    /** M 目录 / C 菜单 / F 按钮 */
    private String menuType;

    private String path;

    private String component;

    private String perms;

    private String icon;

    private Integer sortNo;

    private Integer visible;

    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
