package com.zuche.module.system.dto;

import com.zuche.common.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 操作日志查询参数
 *
 * <p>日期用 LocalDate 而非 LocalDateTime：运营排查从来只说「昨天下午到今天」，
 * 不会精确到秒；服务端把 endDate 当天下班时刻处理（&lt; endDate + 1 天）。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "操作日志查询参数")
public class OperationLogQueryDTO extends PageQuery {

    @Schema(description = "操作人 / 操作描述 / 请求地址，模糊匹配")
    private String keyword;

    @Schema(description = "业务模块，如「公告管理」")
    private String module;

    @Schema(description = "状态：1 成功 / 0 失败，为空查全部")
    private Integer status;

    @Schema(description = "开始日期（含）")
    private LocalDate startDate;

    @Schema(description = "结束日期（含）")
    private LocalDate endDate;
}
