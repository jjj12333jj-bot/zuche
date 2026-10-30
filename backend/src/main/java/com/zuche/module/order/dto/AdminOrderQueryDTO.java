package com.zuche.module.order.dto;

import com.zuche.common.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 平台订单查询参数
 *
 * <p>与网点端的 {@code StoreOrderQueryDTO} 分开：平台端多了 storeId 和日期区间，
 * 少了「只能看本网点」的隐含约束。共用会导致平台端不小心带上 storeId 过滤。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "平台订单查询参数")
public class AdminOrderQueryDTO extends PageQuery {

    @Schema(description = "订单状态，为空查全部")
    private String status;

    @Schema(description = "取车网点，为空查全部网点")
    private Long storeId;

    @Schema(description = "订单号 / 客户账号 / 姓名 / 手机号，模糊匹配")
    private String keyword;

    @Schema(description = "取车日期起（含）")
    private LocalDate startFrom;

    @Schema(description = "取车日期止（含）")
    private LocalDate startTo;
}
