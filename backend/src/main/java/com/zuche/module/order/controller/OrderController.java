package com.zuche.module.order.controller;

import com.zuche.common.PageResult;
import com.zuche.common.Result;
import com.zuche.module.order.dto.OrderCancelDTO;
import com.zuche.module.order.dto.OrderCreateDTO;
import com.zuche.module.order.dto.OrderPayDTO;
import com.zuche.module.order.dto.OrderQueryDTO;
import com.zuche.module.order.dto.OrderQuoteDTO;
import com.zuche.module.order.service.OrderService;
import com.zuche.module.order.vo.OrderCreateVO;
import com.zuche.module.order.vo.OrderCancelVO;
import com.zuche.module.order.vo.OrderDetailVO;
import com.zuche.module.order.vo.OrderListVO;
import com.zuche.module.order.vo.OrderPayVO;
import com.zuche.module.order.vo.OrderQuoteVO;
import com.zuche.module.order.vo.OrderReturnVO;
import io.swagger.v3.oas.annotations.Operation;
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
 * 租赁订单接口
 *
 * <p>所有接口都要求登录（不在白名单里）。数据隔离靠 {@code user_id = 当前登录人}，
 * 而不是靠前端不显示别人的订单——越权查详情会直接查不到。</p>
 */
@Tag(name = "03-订单", description = "试算、下单、模拟支付、取消、我的订单")
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @Operation(summary = "租赁试算", description = "按当前用户信用等级实时计算租金与押金，不落库")
    @PostMapping("/lease-quote")
    public Result<OrderQuoteVO> quote(@Valid @RequestBody OrderQuoteDTO dto) {
        return Result.success(orderService.quote(dto));
    }

    @Operation(summary = "创建订单", description = "生成待支付订单，金额由后端按数据库价格重算")
    @PostMapping
    public Result<OrderCreateVO> create(@Valid @RequestBody OrderCreateDTO dto) {
        return Result.success("下单成功，请尽快完成支付", orderService.create(dto));
    }

    @Operation(summary = "我的订单分页", description = "status 为空查全部，可按状态筛选")
    @GetMapping
    public Result<PageResult<OrderListVO>> myOrders(@ParameterObject @ModelAttribute OrderQueryDTO query) {
        return Result.success(orderService.myOrders(query));
    }

    @Operation(summary = "订单详情", description = "含明细与支付流水（租金/押金两条）")
    @GetMapping("/{id}")
    public Result<OrderDetailVO> detail(@PathVariable Long id) {
        return Result.success(orderService.detail(id));
    }

    @Operation(summary = "模拟支付", description = "待支付 → 待取车；生成 RENT + DEPOSIT 两条流水并锁定车辆")
    @PostMapping("/{id}/pay")
    public Result<OrderPayVO> pay(@PathVariable Long id,
                                  @Valid @RequestBody(required = false) OrderPayDTO dto) {
        return Result.success("模拟支付成功", orderService.pay(id, dto));
    }

    @Operation(summary = "取消订单",
            description = "待支付：仅释放车辆；待取车（已支付）：退还租金+押金、释放车辆、扣信用分")
    @PostMapping("/{id}/cancel")
    public Result<OrderCancelVO> cancel(@PathVariable Long id,
                                        @RequestBody(required = false) OrderCancelDTO dto) {
        OrderCancelVO vo = orderService.cancel(id, dto == null ? null : dto.getReason());
        String message = vo.getRefundAmount() != null && vo.getRefundAmount().signum() > 0
                ? "订单已取消，退款 " + vo.getRefundAmount() + " 元"
                : "订单已取消";
        return Result.success(message, vo);
    }

    @Operation(summary = "确认取车", description = "待取车 → 已取车；开始计算租期")
    @PostMapping("/{id}/pickup")
    public Result<Void> pickup(@PathVariable Long id) {
        orderService.pickup(id);
        return Result.success("已确认取车，祝您用车愉快", null);
    }

    @Operation(summary = "申请归还", description = "已取车/逾期 → 待归还")
    @PostMapping("/{id}/return-apply")
    public Result<Void> applyReturn(@PathVariable Long id) {
        orderService.applyReturn(id);
        return Result.success("已提交归还申请", null);
    }

    @Operation(summary = "确认归还并结算",
            description = "待归还/逾期 → 已完成；按逾期天数扣违约金后退还押金，并结算信用分")
    @PostMapping("/{id}/return-confirm")
    public Result<OrderReturnVO> confirmReturn(@PathVariable Long id) {
        OrderReturnVO vo = orderService.confirmReturn(id);
        String message = vo.getLateFee() != null && vo.getLateFee().signum() > 0
                ? "归还完成，扣除逾期违约金 " + vo.getLateFee() + " 元，实退押金 " + vo.getDepositRefunded() + " 元"
                : "归还完成，押金已全额退还";
        return Result.success(message, vo);
    }
}
