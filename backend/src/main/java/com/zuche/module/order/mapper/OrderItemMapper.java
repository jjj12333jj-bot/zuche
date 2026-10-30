package com.zuche.module.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zuche.module.order.entity.OrderItem;
import com.zuche.module.order.vo.OrderItemVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 订单明细 Mapper
 */
@Mapper
public interface OrderItemMapper extends BaseMapper<OrderItem> {

    @Select("SELECT id, vehicle_id, vehicle_name, license_plate, daily_price, quantity, "
            + "rent_days, discount_rate, rent_amount, deposit_amount "
            + "FROM order_item WHERE order_id = #{orderId} ORDER BY id")
    List<OrderItemVO> selectItemVO(@Param("orderId") Long orderId);
}
