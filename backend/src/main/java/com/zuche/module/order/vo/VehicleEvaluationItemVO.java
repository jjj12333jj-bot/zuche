package com.zuche.module.order.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 车辆详情页的评价条目（前台公开）
 *
 * <p>与后台的 {@link EvaluationVO} 分开定义，是因为前台不该看到手机号、订单号、网点内部信息。
 * 少一个字段就少一次泄漏，宁可多写一个类，也不靠前端"记得别渲染"。</p>
 */
@Data
@Schema(description = "前台评价条目")
public class VehicleEvaluationItemVO {

    private Long id;

    @Schema(description = "评价人昵称，已脱敏如「张*伟」")
    private String customerName;

    @Schema(description = "综合评分 1~5")
    private Integer rating;

    @Schema(description = "车辆状况评分 1~5")
    private Integer vehicleRating;

    @Schema(description = "门店服务评分 1~5")
    private Integer storeRating;

    private String content;

    @Schema(description = "图片地址数组")
    private List<String> images;

    @Schema(description = "商家解释性回复")
    private String reply;

    private LocalDateTime replyTime;

    private LocalDateTime createTime;
}
