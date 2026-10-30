package com.zuche.module.credit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zuche.module.credit.entity.CreditLog;
import com.zuche.module.credit.vo.CreditBehaviorStats;
import com.zuche.module.credit.vo.CreditLogVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 信用日志 Mapper
 *
 * <p>三条统计 SQL 直接打在 lease_order 上：信用分的"行为特征"必须来自真实订单，
 * 不能靠额外维护的计数列（那种列一旦漏更新，分数就永久错了）。</p>
 */
@Mapper
public interface CreditLogMapper extends BaseMapper<CreditLog> {

    /**
     * 行为统计：只统计<b>最近 N 笔已支付订单</b>（N 来自 BEHAVIOR_WINDOW 规则）
     *
     * <p>用滚动窗口而不是全历史：一个有 50 单好记录的用户再取消 1 单，
     * 全历史口径下取消率只涨 2%，行为分几乎不动；窗口口径下是 10%，
     * 分数立刻有反应——风控关心的是"最近这个人靠不靠谱"。</p>
     *
     * <p>口径说明：</p>
     * <ul>
     *   <li>已支付 = pay_time 非空（含之后取消的）</li>
     *   <li>支付后取消 = 已支付且状态 CANCELLED</li>
     *   <li>按时 = 已完成且 overdue_days = 0</li>
     * </ul>
     */
    @Select("""
            SELECT
              COUNT(*)                                                          AS paid_count,
              COALESCE(SUM(CASE WHEN status = 'CANCELLED' THEN 1 ELSE 0 END), 0) AS paid_cancel_count,
              COALESCE(SUM(CASE WHEN status = 'COMPLETED' THEN 1 ELSE 0 END), 0) AS finished_count,
              COALESCE(SUM(CASE WHEN status = 'COMPLETED'
                                 AND COALESCE(overdue_days, 0) = 0 THEN 1 ELSE 0 END), 0) AS on_time_count,
              COALESCE(SUM(CASE WHEN COALESCE(overdue_days, 0) > 0 THEN 1 ELSE 0 END), 0)   AS overdue_count
            FROM (
              SELECT status, overdue_days
              FROM lease_order
              WHERE user_id = #{userId}
                AND deleted = 0
                AND pay_time IS NOT NULL
              ORDER BY id DESC
              LIMIT #{window}
            ) t
            """)
    CreditBehaviorStats selectBehaviorStats(@Param("userId") Long userId,
                                            @Param("window") int window);

    /** 已累计的行为分（Σ change_type = 'BEHAVIOR'），用于算"还差多少分要补" */
    @Select("SELECT COALESCE(SUM(score_change), 0) FROM credit_log "
            + "WHERE user_id = #{userId} AND change_type = 'BEHAVIOR'")
    int sumBehaviorChange(@Param("userId") Long userId);

    /** 该用户是否已经做过行为分基线校准 */
    @Select("SELECT COUNT(*) FROM credit_log WHERE user_id = #{userId} AND change_type = 'BEHAVIOR'")
    int countBehaviorLog(@Param("userId") Long userId);

    /** 全量变动之和，用于把分数重算成 clamp(100 + Σ)，与种子脚本校准口径一致 */
    @Select("SELECT COALESCE(SUM(score_change), 0) FROM credit_log WHERE user_id = #{userId}")
    int sumAllChange(@Param("userId") Long userId);

    /** 幂等预检：同订单同事件同幂等键是否已处理过 */
    @Select("SELECT COUNT(*) FROM credit_log "
            + "WHERE user_id = #{userId} AND action_code = #{actionCode} AND dedup_key = #{dedupKey} "
            + "AND (order_id = #{orderId} OR (#{orderId} IS NULL AND order_id IS NULL))")
    int countByDedup(@Param("userId") Long userId,
                     @Param("orderId") Long orderId,
                     @Param("actionCode") String actionCode,
                     @Param("dedupKey") String dedupKey);

    /** 信用明细分页：左连 credit_rule 取事件名、左连订单取订单号 */
    IPage<CreditLogVO> selectLogPage(Page<CreditLogVO> page,
                                     @Param("userId") Long userId,
                                     @Param("changeType") String changeType);
}
