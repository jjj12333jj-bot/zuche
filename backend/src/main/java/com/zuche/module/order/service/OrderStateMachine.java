package com.zuche.module.order.service;

import com.zuche.common.BusinessException;
import com.zuche.common.ResultCode;
import com.zuche.common.enums.OrderStatus;
import org.springframework.stereotype.Component;

/**
 * 订单状态机执行器
 *
 * <p>{@link OrderStatus} 只描述"允许怎么流转"，本类负责在流转前做断言，
 * 把非法流转翻译成可读的业务异常。所有改状态的地方都必须先过这一关，
 * 不允许出现 {@code order.setStatus(...)} 直接赋值。</p>
 */
@Component
public class OrderStateMachine {

    /**
     * 断言可以从 current 流转到 target，否则抛业务异常
     */
    public void assertTransfer(OrderStatus current, OrderStatus target) {
        if (current == null) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "订单状态异常，无法继续操作");
        }
        if (current == target) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR,
                    "订单已经是「" + target.getLabel() + "」状态，请勿重复操作");
        }
        if (!current.canTransfer(target)) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR,
                    "订单当前为「" + current.getLabel() + "」，不能变更为「" + target.getLabel() + "」");
        }
    }

    /**
     * 断言订单处于指定状态
     */
    public void assertStatus(OrderStatus current, OrderStatus expected, String action) {
        if (current != expected) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR,
                    "仅「" + expected.getLabel() + "」的订单可以" + action
                            + "，当前状态为「" + current.getLabel() + "」");
        }
    }
}
