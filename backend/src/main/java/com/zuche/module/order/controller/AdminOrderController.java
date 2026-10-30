package com.zuche.module.order.controller;

import com.zuche.common.PageResult;
import com.zuche.common.Result;
import com.zuche.module.order.dto.AdminCloseOrderDTO;
import com.zuche.module.order.dto.AdminOrderQueryDTO;
import com.zuche.module.order.service.AdminOrderService;
import com.zuche.module.order.vo.AdminOrderDetailVO;
import com.zuche.module.order.vo.AdminOrderStatsVO;
import com.zuche.module.order.vo.AdminOrderVO;
import com.zuche.module.order.vo.OrderCancelVO;
import com.zuche.security.RequireRole;
import com.zuche.security.RoleCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 平台订单管理（仅系统管理员）
 */
@Tag(name = "10-平台订单", description = "全平台订单查询、详情、关闭")
@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
@RequireRole(RoleCode.SYS_ADMIN)
@SecurityRequirement(name = "Authorization")
public class AdminOrderController {

    private final AdminOrderService adminOrderService;

    @Operation(summary = "订单统计", description = "各状态计数与今日新增，顶部数字卡用")
    @GetMapping("/stats")
    public Result<AdminOrderStatsVO> stats() {
        return Result.success(adminOrderService.stats());
    }

    @Operation(summary = "订单分页", description = "全平台，可按状态/网点/关键字/取车日期筛选")
    @GetMapping
    public Result<PageResult<AdminOrderVO>> page(@ParameterObject @ModelAttribute AdminOrderQueryDTO query) {
        return Result.success(adminOrderService.page(query));
    }

    @Operation(summary = "订单详情", description = "含明细、支付流水、退款流水、违约金预估")
    @GetMapping("/{id}")
    public Result<AdminOrderDetailVO> detail(@Parameter(description = "订单 ID") @PathVariable Long id) {
        return Result.success(adminOrderService.detail(id));
    }

    @Operation(summary = "关闭订单", description = "仅待支付/待取车；已支付的自动全额退款并释放车辆")
    @PostMapping("/{id}/close")
    public Result<OrderCancelVO> close(@Parameter(description = "订单 ID") @PathVariable Long id,
                                       @Valid @RequestBody AdminCloseOrderDTO dto) {
        return Result.success("订单已关闭", adminOrderService.close(id, dto.getReason()));
    }
}
