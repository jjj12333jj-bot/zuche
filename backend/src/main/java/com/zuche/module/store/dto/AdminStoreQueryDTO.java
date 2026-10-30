package com.zuche.module.store.dto;

import com.zuche.common.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 平台网点查询参数
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "平台网点查询参数")
public class AdminStoreQueryDTO extends PageQuery {

    @Schema(description = "网点名称 / 编码 / 地址 / 电话，模糊匹配")
    private String keyword;

    @Schema(description = "状态：1 营业 / 0 停用，为空查全部")
    private Integer status;
}
