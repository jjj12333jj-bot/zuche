package com.zuche.module.risk.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 逾期扫描结果
 *
 * <p>定时任务每天跑一次，但答辩现场不可能等到凌晨 1 点，
 * 所以后台提供"立即扫描"按钮复用同一段逻辑，返回本次扫描的真实统计。</p>
 */
@Data
@Schema(description = "逾期扫描结果")
public class OverdueScanVO {

    private LocalDateTime scanTime;

    @Schema(description = "扫描到的超期未还订单数")
    private Integer candidateCount = 0;

    @Schema(description = "本次置为逾期的订单数")
    private Integer markedCount = 0;

    @Schema(description = "新增预警数")
    private Integer warnedCount = 0;

    @Schema(description = "新增黑名单数")
    private Integer blacklistedCount = 0;

    @Schema(description = "到期自动解除的名单数")
    private Integer releasedCount = 0;

    @Schema(description = "逐单处理明细")
    private List<String> details = new ArrayList<>();
}
