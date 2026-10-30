package com.zuche.module.order.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zuche.common.BusinessException;
import com.zuche.common.PageResult;
import com.zuche.common.ResultCode;
import com.zuche.common.enums.OrderStatus;
import com.zuche.module.credit.service.CreditService;
import com.zuche.module.order.dto.AdminOrderQueryDTO;
import com.zuche.module.order.mapper.AdminOrderMapper;
import com.zuche.module.order.mapper.OrderItemMapper;
import com.zuche.module.order.vo.AdminOrderDetailVO;
import com.zuche.module.order.vo.AdminOrderStatsVO;
import com.zuche.module.order.vo.AdminOrderVO;
import com.zuche.module.order.vo.OrderCancelVO;
import com.zuche.module.payment.service.PaymentService;
import com.zuche.module.refund.service.RefundService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Map;

/**
 * 平台订单服务
 *
 * <p>只做「看」和「关」，不做取车/还车——那是网点的工作，平台越俎代庖会让
 * 履约责任人变得模糊。真需要改状态时，走「关闭订单」这条唯一的破坏性入口。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminOrderService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private final AdminOrderMapper adminOrderMapper;
    private final OrderItemMapper orderItemMapper;
    private final PaymentService paymentService;
    private final RefundService refundService;
    private final CreditService creditService;
    private final OrderService orderService;

    /* ==================== 查询 ==================== */

    public PageResult<AdminOrderVO> page(AdminOrderQueryDTO query) {
        Page<AdminOrderVO> page = new Page<>(query.getCurrent(), query.getPageSize());
        IPage<AdminOrderVO> result = adminOrderMapper.selectAdminOrderPage(page, query);

        Map<String, String> levelLabels = creditService.levelLabelMap();
        result.getRecords().forEach(vo -> {
            OrderStatus status = OrderStatus.of(vo.getStatus());
            vo.setStatusLabel(status.getLabel());
            String level = vo.getCreditLevelSnapshot();
            vo.setCreditLevelLabel(levelLabels.getOrDefault(level, level));
            // 平台端只有「关闭」一个破坏性动作，且只对未取车的单开放
            boolean closable = status == OrderStatus.WAIT_PAY || status == OrderStatus.WAIT_PICKUP;
            vo.setCanCancel(closable);
            vo.setCanPay(false);
            vo.setCanPickup(false);
            vo.setCanApplyReturn(false);
            vo.setCanConfirmReturn(false);
        });
        return PageResult.of(result);
    }

    public AdminOrderStatsVO stats() {
        AdminOrderStatsVO vo = adminOrderMapper.selectStats();
        if (vo == null) {
            vo = new AdminOrderStatsVO();
        }
        vo.setTotal(nz(vo.getTotal()));
        vo.setPendingPay(nz(vo.getPendingPay()));
        vo.setPendingPickup(nz(vo.getPendingPickup()));
        vo.setInUse(nz(vo.getInUse()));
        vo.setOverdue(nz(vo.getOverdue()));
        vo.setTodayNew(nz(vo.getTodayNew()));
        vo.setLateFeeTotal(vo.getLateFeeTotal() == null ? ZERO : vo.getLateFeeTotal());
        return vo;
    }

    public AdminOrderDetailVO detail(Long orderId) {
        AdminOrderDetailVO vo = adminOrderMapper.selectAdminOrderDetail(orderId);
        if (vo == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "订单不存在");
        }
        Map<String, String> levelLabels = creditService.levelLabelMap();
        OrderStatus status = OrderStatus.of(vo.getStatus());
        vo.setStatusLabel(status.getLabel());
        String level = vo.getCreditLevelSnapshot();
        vo.setCreditLevelLabel(levelLabels.getOrDefault(level, level));

        vo.setItems(orderItemMapper.selectItemVO(orderId));
        vo.setPayments(paymentService.listByOrder(orderId));
        vo.setRefunds(refundService.listByOrder(orderId));

        // 违约金只在归还结算那一刻落账。未结算的单给「按当前规则算出来的预估」，
        // 否则平台会看到一堆 0 而误以为逾期没有成本
        boolean settled = status == OrderStatus.COMPLETED || status == OrderStatus.RETURNED;
        vo.setLateFeeSettled(settled);
        if (!settled) {
            int overdueDays = calcOverdueDays(vo.getEndDate(), LocalDate.now());
            vo.setOverdueDays(overdueDays);
            vo.setLateFee(calcLateFee(vo.getDepositAmount(), overdueDays));
        }

        // 押金标准合计 = 实付押金 ÷ 系数，反推「没有信用折扣时要交多少」
        BigDecimal coefficient = vo.getDepositCoefficient();
        if (coefficient != null && coefficient.signum() > 0 && vo.getDepositAmount() != null) {
            BigDecimal standard = vo.getDepositAmount()
                    .divide(coefficient, 4, RoundingMode.HALF_UP)
                    .setScale(2, RoundingMode.HALF_UP);
            vo.setDepositStandardTotal(standard);
            vo.setDepositSaved(money(standard.subtract(vo.getDepositAmount())));
        }
        return vo;
    }

    /* ==================== 关闭 ==================== */

    /**
     * 平台关闭订单
     *
     * <p>复用用户端取消的同一套资金与车辆处理，只在两处不同：</p>
     * <ul>
     *   <li>不校验归属（平台可以关任何网点的单）</li>
     *   <li>不自动扣信用分——管理员关闭的原因无法判断责任方（可能是门店无车、
     *       也可能是客户电话要求），要扣分得走「信用调整」单独留痕</li>
     * </ul>
     */
    public OrderCancelVO close(Long orderId, String reason) {
        if (!StringUtils.hasText(reason)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "关闭原因不能为空");
        }
        OrderCancelVO vo = orderService.adminClose(orderId, reason.trim());
        log.info("平台关闭订单: orderNo={}, reason={}", vo.getOrderNo(), reason);
        return vo;
    }

    /* ==================== 金额工具 ==================== */

    /** 与 OrderService 同源：规则本身来自 credit_rule，这里只是读规则的一小段代码 */
    private BigDecimal calcLateFee(BigDecimal deposit, int overdueDays) {
        if (overdueDays <= 0 || deposit == null || deposit.signum() <= 0) {
            return ZERO;
        }
        int daily = creditService.param("LATE_FEE_DAILY", 0);
        if (daily <= 0) {
            return ZERO;
        }
        int maxDays = creditService.maxTimes("LATE_FEE_DAILY");
        int days = Math.min(overdueDays, maxDays);
        BigDecimal fee = BigDecimal.valueOf(daily).multiply(BigDecimal.valueOf(days));
        return money(fee.min(deposit));
    }

    private int calcOverdueDays(LocalDate endDate, LocalDate returnDate) {
        if (endDate == null || returnDate == null || !returnDate.isAfter(endDate)) {
            return 0;
        }
        return (int) ChronoUnit.DAYS.between(endDate, returnDate);
    }

    private BigDecimal money(BigDecimal value) {
        return value == null ? ZERO : value.setScale(2, RoundingMode.HALF_UP);
    }

    private static int nz(Integer value) {
        return value == null ? 0 : value;
    }
}
