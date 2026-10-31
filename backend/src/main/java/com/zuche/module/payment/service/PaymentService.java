package com.zuche.module.payment.service;

import com.zuche.common.BusinessException;
import com.zuche.common.enums.PayType;
import com.zuche.module.order.entity.LeaseOrder;
import com.zuche.module.order.vo.PaymentVO;
import com.zuche.module.payment.entity.PaymentRecord;
import com.zuche.module.payment.mapper.PaymentRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 模拟支付服务
 *
 * <p>不接任何真实支付 SDK。一次支付成功生成两条流水：</p>
 * <ul>
 *   <li>{@code RENT} 租金 —— 计入营收</li>
 *   <li>{@code DEPOSIT} 押金 —— 代收待退，不是收入</li>
 * </ul>
 *
 * <p>拆两条而不是合并一条，是为了让"营收"和"押金池"两个口径都能直接从流水表算出来，
 * 运营看板的营收不会被押金污染。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final DateTimeFormatter PAY_NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final String DEFAULT_CHANNEL = "MOCK_WECHAT";

    private final PaymentRecordMapper paymentMapper;

    /**
     * 生成模拟支付流水（调用方必须在事务内调用）
     *
     * @return 本次落库的流水，按 租金 → 押金 顺序返回
     */
    public List<PaymentRecord> mockPay(LeaseOrder order, String payMethod) {
        String channel = (payMethod == null || payMethod.isBlank()) ? DEFAULT_CHANNEL : payMethod;
        LocalDateTime now = LocalDateTime.now();

        List<PaymentRecord> records = new ArrayList<>(2);
        records.add(build(order, PayType.RENT, order.getRentAmount(), channel, now));
        records.add(build(order, PayType.DEPOSIT, order.getDepositAmount(), channel, now));

        for (PaymentRecord record : records) {
            paymentMapper.insert(record);
        }
        log.info("模拟支付成功: orderNo={}, 租金={}, 押金={}, 渠道={}",
                order.getOrderNo(), order.getRentAmount(), order.getDepositAmount(), channel);
        return records;
    }

    /**
     * 查询订单的全部流水（含未来的退款单，按 id 升序）
     */
    public List<PaymentVO> listByOrder(Long orderId) {
        List<PaymentVO> list = paymentMapper.selectPaymentVO(orderId);
        for (PaymentVO vo : list) {
            vo.setPayTypeLabel(payTypeLabel(vo.getPayType()));
        }
        return list;
    }

    public String payTypeLabel(String payType) {
        try {
            return PayType.valueOf(payType).getLabel();
        } catch (IllegalArgumentException e) {
            return payType;
        }
    }

    private PaymentRecord build(LeaseOrder order, PayType type, java.math.BigDecimal amount,
                                String channel, LocalDateTime now) {
        PaymentRecord record = new PaymentRecord();
        record.setPayNo(generatePayNo(type));
        record.setOrderId(order.getId());
        record.setUserId(order.getUserId());
        record.setPayType(type.name());
        record.setAmount(amount);
        record.setPayMethod(channel);
        record.setStatus("SUCCESS");
        record.setPayTime(now);
        record.setRemark("订单支付-" + type.getLabel());
        record.setCreateTime(now);
        record.setUpdateTime(now);
        return record;
    }

    /**
     * 流水号：MOCK + 时间戳 + 类型首字母 + 6 位随机，长度 25，唯一索引兜底
     */
    private String generatePayNo(PayType type) {
        String suffix = String.format("%06d", ThreadLocalRandom.current().nextInt(1_000_000));
        return "PAY" + LocalDateTime.now().format(PAY_NO_FMT) + type.name().charAt(0) + suffix;
    }

    /**
     * 取某类支付成功流水（退款时用来回指原流水）
     *
     * @param orderId 订单
     * @param type    RENT / DEPOSIT
     */
    public PaymentRecord findSuccessPayment(Long orderId, PayType type) {
        return paymentMapper.selectSuccessByType(orderId, type.name());
    }

    /**
     * 校验订单是否已经支付过（幂等保护）
     */
    public void assertNotPaid(Long orderId) {
        if (paymentMapper.countSuccessByOrder(orderId) > 0) {
            throw new BusinessException("该订单已支付成功，请勿重复支付");
        }
    }
}
