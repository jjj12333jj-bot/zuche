package com.zuche.module.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 首页轮播图（对应 banner）
 *
 * <p>只存图片地址，不存图片二进制——演示环境没有对象存储，
 * seed 用本地 /images/banners/*.svg 占位；接入 OSS 后把 image_url 换成外链即可，
 * 表结构与接口都不用动。</p>
 */
@Data
@TableName("banner")
public class Banner {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String imageUrl;

    private String linkUrl;

    /** HOME 首页 / PORTAL 列表页 */
    private String position;

    private Integer sortNo;

    /** 1 显示 / 0 隐藏 */
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
