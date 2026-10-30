package com.zuche.module.refund.service;

import com.zuche.module.order.entity.LeaseOrder;
import com.zuche.module.refund.entity.RefundRecord;
import com.zuche.module.refund.mapper.RefundRecordMapper;
import com.zuche.module.refund.vo.RefundVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 模拟退款服务
 *
 * <p>不接真实支付网关，退款一步到账（PENDING → REFUNDED 在同一事务内完成），
 * 但<b>流水结构按真实场景建</b>：退款单独立编号、回指原支付流水、区分退款类型。
 * 这样以后接真实网关时，只需要把"立即成功"改成"异步回调成功"，表结构不用动。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefundService {

    private static final DateTimeFormatter NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final String CHANNEL = "MOCK_WECHAT";
    private static final BigDecimal ZERO = new BigDecimal("0.00");

    private final RefundRecordMapper refundMapper;

    /**
     * 生成一条退款流水（调用方必须在事务内调用）
     *
     * @param order     订单
     * @param refundType DEPOSIT / CANCEL / LATE_FEE
     * @param amount    退款金额
     * @param paymentId 原支付流水 ID，可为空
     * @param reason    退款原因，会展示在订单详情
     * @return 落库后的流水；金额 <= 0 时不生成流水，返回 null（例如零押金订单）
     */
    public RefundRecord refund(LeaseOrder order, String refundType, BigDecimal amount,
                               Long paymentId, String reason) {
        if (amount == null || amount.compareTo(ZERO) <= 0) {
            log.info("退款金额为 0，跳过生成流水: orderNo={}, type={}", order.getOrderNo(), refundType);
            return null;
        }

        LocalDateTime now = LocalDateTime.now();
        RefundRecord record = new RefundRecord();
        record.setRefundNo(generateRefundNo());
        record.setOrderId(order.getId());
        record.setPaymentId(paymentId);
        record.setUserId(order.getUserId());
        record.setRefundType(refundType);
        record.setAmount(amount);
        record.setReason(reason);
        record.setStatus("REFUNDED");
        record.setApplyTime(now);
        record.setRefundTime(now);
        record.setChannel(CHANNEL);
        record.setRemark("模拟退款-" + typeLabel(refundType));
        record.setCreateTime(now);
        record.setUpdateTime(now);
        refundMapper.insert(record);

        log.info("模拟退款成功: orderNo={}, refundNo={}, type={}, amount={}",
                order.getOrderNo(), record.getRefundNo(), refundType, amount);
        return record;
    }

    public List<RefundVO> listByOrder(Long orderId) {
        List<RefundVO> list = refundMapper.selectRefundVO(orderId);
        for (RefundVO vo : list) {
            vo.setRefundTypeLabel(typeLabel(vo.getRefundType()));
            vo.setStatusLabel("REFUNDED".equals(vo.getStatus()) ? "已退款" : "处理中");
        }
        return list;
    }

    public String typeLabel(String refundType) {
        if (refundType == null) {
            return "退款";
        }
        return switch (refundType) {
            case "DEPOSIT" -> "押金退还";
            case "CANCEL" -> "取消退款";
            case "LATE_FEE" -> "违约金冲正";
            default -> refundType;
        };
    }

    /** 退款单号：RF + 时间戳 + 6 位随机，配合 uk_refund_no 兜底 */
    private String generateRefundNo() {
        String suffix = String.format("%06d", ThreadLocalRandom.current().nextInt(1_000_000));
        return "RF" + LocalDateTime.now().format(NO_FMT) + suffix;
    }
}
