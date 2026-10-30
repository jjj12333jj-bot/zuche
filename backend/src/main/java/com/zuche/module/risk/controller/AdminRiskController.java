package com.zuche.module.risk.controller;

import com.zuche.common.Result;
import com.zuche.module.risk.service.RiskControlService;
import com.zuche.module.risk.vo.OverdueScanVO;
import com.zuche.security.RequireRole;
import com.zuche.security.RoleCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 逾期风控任务管理（仅系统管理员）
 *
 * <p>之所以额外暴露"立即扫描"：定时任务每天凌晨才跑，答辩现场没法演示。
 * 手动触发与定时任务调用的是同一个 {@link RiskControlService#scanOverdue()}，
 * 顺带还能验证定时任务这段逻辑本身是对的。</p>
 */
@Tag(name = "06-逾期风控任务", description = "手动触发逾期扫描、查看风控概览")
@RestController
@RequestMapping("/api/admin/risk")
@RequiredArgsConstructor
@RequireRole(RoleCode.SYS_ADMIN)
@SecurityRequirement(name = "Authorization")
public class AdminRiskController {

    private final RiskControlService riskControlService;

    @Operation(summary = "立即执行逾期扫描",
            description = "与每日 01:00 定时任务同一段逻辑：超期订单置逾期、写预警/黑名单、解除到期名单")
    @PostMapping("/scan")
    public Result<OverdueScanVO> scan() {
        return Result.success("扫描完成", riskControlService.scanOverdue());
    }

    @Operation(summary = "风控概览", description = "逾期订单数、预警数、黑名单数、待处理申诉数")
    @GetMapping("/overview")
    public Result<Map<String, Object>> overview() {
        return Result.success(riskControlService.overview());
    }
}
