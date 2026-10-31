package com.zuche.module.order.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 评价列表项
 *
 * <p>一条评价要能独立看懂：谁、在哪个网点、租了哪台车、打了几分、说了什么、有没有回复。
 * 因此除了评价表本身，还要 JOIN 订单（订单号、网点）、用户（账号姓名）、车辆（品牌车型车牌）。</p>
 */
@Data
@Schema(description = "评价列表项")
public class EvaluationVO {

    private Long id;

    private Long orderId;

    private String orderNo;

    private Long storeId;

    private String storeName;

    private Long userId;

    private String username;

    private String customerName;

    private String customerPhone;

    private Long vehicleId;

    @Schema(description = "品牌 + 车型，如 大众 朗逸")
    private String vehicleName;

    private String licensePlate;

    @Schema(description = "综合评分 1~5，车辆与门店两项平均")
    private Integer rating;

    @Schema(description = "车辆状况评分 1~5")
    private Integer vehicleRating;

    @Schema(description = "门店服务评分 1~5")
    private Integer storeRating;

    @Schema(description = "综合星级文案，如「非常满意」")
    private String ratingLabel;

    private String content;

    @Schema(description = "图片地址数组，库里存 JSON 字符串，服务层解析成 List")
    private List<String> images;

    private String reply;

    private LocalDateTime replyTime;

    @Schema(description = "1 展示 0 已隐藏")
    private Integer status;

    private String statusLabel;

    private LocalDateTime createTime;
}
