package com.zuche.module.dashboard.dto;

import lombok.Data;

/**
 * 漏斗原始计数（一行五列）
 *
 * <p>每一级都用订单自己的时间戳判定，而不是用当前 status 推断：
 * 一单被取消后 status 会变，但 pay_time / pickup_time 仍然是历史事实。</p>
 */
@Data
public class FunnelRawDTO {

    /** 下单数 */
    private Integer orderTotal;

    /** 已支付 */
    private Integer paid;

    /** 已取车 */
    private Integer picked;

    /** 已归还 */
    private Integer returned;

    /** 已完成（结算完毕） */
    private Integer completed;
}
