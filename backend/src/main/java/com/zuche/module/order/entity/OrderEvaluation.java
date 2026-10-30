package com.zuche.module.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 订单评价（对应 order_evaluation）
 *
 * <p>一条订单最多一条评价，靠 {@code uk_order} 唯一索引兜底，不靠业务层判断——
 * 两个标签页同时点提交时，先到的那条插入成功，后到的直接被数据库拦下。</p>
 *
 * <p>status 只控制「前台是否展示」，不做逻辑删除：差评被隐藏后运营仍要能查到原始内容，
 * 真删了就没法复盘。逻辑删除只用于用户自己撤回。</p>
 */
@Data
@TableName("order_evaluation")
@Schema(description = "订单评价")
public class OrderEvaluation {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;

    private Long userId;

    private Long vehicleId;

    /** 1~5 星 */
    private Integer rating;

    private String content;

    /** 图片地址，JSON 数组字符串，如 ["/images/cars/car-1.svg"] */
    private String images;

    /** 网点回复 */
    private String reply;

    private LocalDateTime replyTime;

    /** 1 展示 0 已隐藏 */
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
