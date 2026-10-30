package com.zuche.module.dashboard.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 通用的"名称 + 数量"结构，用于车辆状态分布等简单维度
 */
@Data
@Schema(description = "名称数量对")
public class NameValueVO {

    @Schema(description = "名称")
    private String name;

    @Schema(description = "数值")
    private Integer value;

    @Schema(description = "占比（%）")
    private Double ratio;

    public NameValueVO() {
    }

    public NameValueVO(String name, Integer value) {
        this.name = name;
        this.value = value;
    }
}
