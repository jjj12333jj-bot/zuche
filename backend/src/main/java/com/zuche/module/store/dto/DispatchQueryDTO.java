package com.zuche.module.store.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 调车申请查询参数
 */
@Data
@Schema(description = "调车申请查询参数")
public class DispatchQueryDTO {

    @Schema(description = "状态：PENDING / APPROVED / REJECTED / CANCELLED，为空查全部")
    private String status;

    @Schema(description = "方向（仅网点端有效）：OUT 本网点调出 / IN 调入本网点，为空查两个方向")
    private String direction;

    @Schema(description = "申请单号 / 车牌 / 车辆编号，模糊匹配")
    private String keyword;

    @Schema(description = "页码，从 1 开始", defaultValue = "1")
    private Integer page = 1;

    @Schema(description = "每页条数，最大 50", defaultValue = "10")
    private Integer size = 10;

    public long getCurrent() {
        return page == null || page < 1 ? 1 : page;
    }

    public long getPageSize() {
        if (size == null || size < 1) {
            return 10;
        }
        return Math.min(size, 50);
    }
}
