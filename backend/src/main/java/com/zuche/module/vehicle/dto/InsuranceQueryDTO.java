package com.zuche.module.vehicle.dto;

import com.zuche.common.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 保险记录查询参数（platform 端）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "保险记录查询参数")
public class InsuranceQueryDTO extends PageQuery {

    @Schema(description = "保单类型：COMPULSORY 交强险 / COMMERCIAL 商业险 / PASSENGER 座位险")
    private String insuranceType;

    @Schema(description = "网点 ID")
    private Long storeId;

    @Schema(description = "保单状态：1 生效中 0 已失效")
    private Integer status;

    @Schema(description = "车牌 / 保单号 / 保险公司，模糊匹配")
    private String keyword;

    @Schema(description = "只看 30 天内到期的保单")
    private Boolean expiringSoon;
}
