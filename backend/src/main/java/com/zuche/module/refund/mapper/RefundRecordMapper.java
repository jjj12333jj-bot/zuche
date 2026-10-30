package com.zuche.module.refund.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zuche.module.refund.entity.RefundRecord;
import com.zuche.module.refund.vo.RefundVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 退款流水 Mapper
 */
@Mapper
public interface RefundRecordMapper extends BaseMapper<RefundRecord> {

    @Select("SELECT id, refund_no, payment_id, refund_type, amount, reason, status, refund_time "
            + "FROM refund_record WHERE order_id = #{orderId} ORDER BY id")
    List<RefundVO> selectRefundVO(@Param("orderId") Long orderId);
}
