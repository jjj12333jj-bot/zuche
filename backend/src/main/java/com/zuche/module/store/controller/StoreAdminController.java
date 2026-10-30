package com.zuche.module.store.controller;

import com.zuche.common.PageResult;
import com.zuche.common.Result;
import com.zuche.module.order.vo.OrderReturnVO;
import com.zuche.module.store.dto.StoreOrderQueryDTO;
import com.zuche.module.store.service.StoreAdminService;
import com.zuche.module.store.vo.StoreOrderVO;
import com.zuche.module.store.vo.StoreOverviewVO;
import com.zuche.module.system.annotation.OpLog;
import com.zuche.security.RequireRole;
import com.zuche.security.RoleCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 网点工作台（仅网点管理员）
 *
 * <p>数据隔离靠 {@code sys_user.store_id}：所有查询都以当前登录人的网点为过滤条件，
 * 越权访问别的网点订单直接查不到，不依赖前端不传 ID。</p>
 */
@Tag(name = "06-网点工作台", description = "网点概览、本网点订单、交付与归还办理")
@RestController
@RequestMapping("/api/store")
@RequiredArgsConstructor
@RequireRole(RoleCode.STORE_ADMIN)
@SecurityRequirement(name = "Authorization")
public class StoreAdminController {

    private final StoreAdminService storeAdminService;

    @Operation(summary = "网点概览",
            description = "车辆存量、订单待办、今日与本月经营量；全部限定当前登录人所属网点")
    @GetMapping("/overview")
    public Result<StoreOverviewVO> overview() {
        return Result.success(storeAdminService.overview());
    }

    @Operation(summary = "本网点订单分页",
            description = "支持按状态筛选与订单号/客户关键词搜索，只返回本网点订单")
    @GetMapping("/orders")
    public Result<PageResult<StoreOrderVO>> orders(
            @ParameterObject @ModelAttribute StoreOrderQueryDTO query) {
        return Result.success(storeAdminService.orders(query));
    }

    @Operation(summary = "办理交付", description = "待取车 → 已取车；只能操作本网点订单")
    @PostMapping("/orders/{id}/pickup")
    @OpLog(module = "网点订单", operation = "确认交付车辆")
    public Result<Void> pickup(@Parameter(description = "订单 ID") @PathVariable Long id) {
        storeAdminService.pickup(id);
        return Result.success("已办理交付", null);
    }

    @Operation(summary = "办理归还并结算",
            description = "待归还/逾期 → 已完成；违约金、押金退款、信用结算与用户端共用同一套实现")
    @PostMapping("/orders/{id}/return-confirm")
    @OpLog(module = "网点订单", operation = "确认归还并结算")
    public Result<OrderReturnVO> confirmReturn(@Parameter(description = "订单 ID") @PathVariable Long id) {
        OrderReturnVO vo = storeAdminService.confirmReturn(id);
        String message = vo.getLateFee() != null && vo.getLateFee().signum() > 0
                ? "归还完成，扣除逾期违约金 " + vo.getLateFee() + " 元，实退押金 " + vo.getDepositRefunded() + " 元"
                : "归还完成，押金已全额退还";
        return Result.success(message, vo);
    }
}
