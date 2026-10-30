package com.zuche.module.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zuche.module.order.vo.PaymentVO;
import com.zuche.module.payment.entity.PaymentRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 支付流水 Mapper
 */
@Mapper
public interface PaymentRecordMapper extends BaseMapper<PaymentRecord> {

    @Select("SELECT id, pay_no, pay_type, amount, pay_method, status, pay_time, remark "
            + "FROM payment_record WHERE order_id = #{orderId} ORDER BY id")
    List<PaymentVO> selectPaymentVO(@Param("orderId") Long orderId);

    @Select("SELECT COUNT(*) FROM payment_record WHERE order_id = #{orderId} AND status = 'SUCCESS'")
    int countSuccessByOrder(@Param("orderId") Long orderId);

    /** 取指定类型的支付成功流水（退款要回指原流水） */
    @Select("SELECT * FROM payment_record WHERE order_id = #{orderId} AND pay_type = #{payType} "
            + "AND status = 'SUCCESS' ORDER BY id LIMIT 1")
    PaymentRecord selectSuccessByType(@Param("orderId") Long orderId,
                                      @Param("payType") String payType);
}
