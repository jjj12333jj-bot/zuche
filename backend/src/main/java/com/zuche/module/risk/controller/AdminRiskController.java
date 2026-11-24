package com.zuche.module.risk.controller;

import com.zuche.common.Result;
import com.zuche.module.order.service.PickupTimeoutService;
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

import java.util.List;
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
    private final PickupTimeoutService pickupTimeoutService;

    @Operation(summary = "立即执行风控扫描",
            description = "与每日定时任务同一段逻辑：①超期未还订单置逾期、写预警/黑名单、解除到期名单；"
                    + "②超过取车日仍未取车的订单自动取消、退款、释放车辆并扣信用分")
    @PostMapping("/scan")
    public Result<OverdueScanVO> scan() {
        OverdueScanVO vo = riskControlService.scanOverdue();
        List<String> cancelled = pickupTimeoutService.scan();
        for (String orderNo : cancelled) {
            vo.getDetails().add(orderNo + " 超时未取车，已自动取消并退款");
        }
        return Result.success("扫描完成：逾期更新 " + vo.getMarkedCount()
                + " 单，超时未取车取消 " + cancelled.size() + " 单", vo);
    }

    @Operation(summary = "风控概览", description = "逾期订单数、预警数、黑名单数、待处理申诉数")
    @GetMapping("/overview")
    public Result<Map<String, Object>> overview() {
        return Result.success(riskControlService.overview());
    }
}
