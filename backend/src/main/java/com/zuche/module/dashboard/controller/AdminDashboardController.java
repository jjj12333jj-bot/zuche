package com.zuche.module.dashboard.controller;

import com.zuche.common.Result;
import com.zuche.module.dashboard.service.DashboardService;
import com.zuche.module.dashboard.vo.DashboardVO;
import com.zuche.security.RequireRole;
import com.zuche.security.RoleCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 运营分析看板（仅系统管理员）
 *
 * <p>一次请求返回全部区块：指标卡、营收趋势、履约漏斗、车辆状态、
 * 网点经营、车型排行、信用分布。数据全部来自订单与资金流水聚合，没有一条是写死的。</p>
 */
@Tag(name = "05-运营分析看板", description = "营收趋势、履约漏斗、网点与车型经营、信用分布")
@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
@RequireRole(RoleCode.SYS_ADMIN)
@SecurityRequirement(name = "Authorization")
public class AdminDashboardController {

    private final DashboardService dashboardService;

    @Operation(summary = "看板总览",
            description = "days 为统计窗口天数，默认 30，最大 180")
    @GetMapping("/overview")
    public Result<DashboardVO> overview(
            @Parameter(description = "统计天数，默认 30")
            @RequestParam(value = "days", defaultValue = "30") Integer days) {
        return Result.success(dashboardService.overview(days));
    }
}
