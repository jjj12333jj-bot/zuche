package com.zuche.module.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统公告（对应 announcement）
 *
 * <p>「发布状态」与「发布时间」拆成两个字段而不是用时间是否为 null 来表达状态：
 * 公告下架后仍要保留当初的发布时间（运营复盘要看「这条挂了多久」），
 * 重新发布则刷新时间——所以状态归状态，时间归时间。</p>
 */
@Data
@TableName("announcement")
public class Announcement {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String content;

    /** NOTICE 公告 / ACTIVITY 活动 / RULE 规则 */
    private String type;

    private Long publisherId;

    /** 1 已发布 / 0 草稿 */
    private Integer status;

    private Integer viewCount;

    /** 首次发布时写入；下架不清空，再次发布刷新 */
    private LocalDateTime publishTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
