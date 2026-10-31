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
 * <p>两级处置，别混用：</p>
 * <ul>
 *   <li>{@code status=0} 隐藏：评价本身没问题，只是不适合挂在首页（比如提到具体人名），
 *       随时可以恢复，后台照常能查。</li>
 *   <li>{@code deleted=1} 删除：仅系统管理员可用，针对辱骂、广告、泄露隐私等违规内容。
 *       走逻辑删除而不是物理删除，是为了留痕——删了什么、谁删的，日志里能对上。</li>
 * </ul>
 *
 * <p>网点管理员只有回复权，没有删除权：让商家自己删差评，等于既当运动员又当裁判。</p>
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

    /** 综合分：车辆与门店两项平均，四舍五入。统计、排序、榜单统一用这个字段 */
    private Integer rating;

    /** 车辆状况 1~5 星 */
    private Integer vehicleRating;

    /** 门店服务 1~5 星 */
    private Integer storeRating;

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
