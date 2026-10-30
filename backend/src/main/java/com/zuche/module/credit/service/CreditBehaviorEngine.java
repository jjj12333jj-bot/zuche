package com.zuche.module.credit.service;

import com.zuche.module.credit.entity.CreditRule;
import com.zuche.module.credit.vo.CreditBehaviorStats;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

/**
 * 信用行为模型计算器
 *
 * <p><b>为什么需要它：</b>如果信用分只由"完成 +2 / 取消 -3"这类单次事件累加，
 * 那它衡量的只是"做过多少单"，而不是"这个人靠不靠谱"。真正的风控看的是
 * <b>比率特征</b>：同样完成 10 单，一单不逾期和逾期 3 单，风险完全不同。</p>
 *
 * <p><b>模型（只罚不奖，0 分为行为满分）：</b></p>
 * <pre>
 *   统计窗口 = 最近 WINDOW 笔已支付订单
 *   onTimeRate = 窗口内按时归还订单数 / 窗口内已完成订单数
 *   cancelRate = 窗口内支付后取消订单数 / 窗口内已支付订单数
 *
 *   raw = W_ON_TIME × onTimeRate
 *       + W_CANCEL  × (1 - cancelRate)
 *       - W_OVERDUE × 窗口内逾期订单数
 *       - (W_ON_TIME + W_CANCEL)
 *
 *   target = clamp(round(raw), -FLOOR, 0)
 * </pre>
 *
 * <p>无历史订单时 onTimeRate 视为 1、cancelRate 视为 0，raw 正好等于 0，
 * 新用户不会被"默认扣分"，这是模型的中性点。</p>
 *
 * <p><b>四个参数（W_ON_TIME / W_CANCEL / W_OVERDUE / FLOOR）全部来自 credit_rule 的
 * BEHAVIOR_* 规则</b>，本类不含任何分值常量，后台改配置即刻生效。</p>
 */
@Component
public class CreditBehaviorEngine {

    /** 规则缺失时的中性值：不给分也不扣分 */
    private static final int DEFAULT_WEIGHT = 0;

    /** 规则缺失时的兜底窗口：只看最近 10 笔，避免小样本噪声 */
    private static final int DEFAULT_WINDOW = 10;

    /**
     * 统计窗口大小（credit_rule 的 BEHAVIOR_WINDOW）
     *
     * <p>窗口越大越平滑、越小越敏感。做成配置项，是为了能按业务阶段调整，
     * 而不是把"10 单"这种经验值焊在代码里。</p>
     */
    public int window(Map<String, CreditRule> rules) {
        int window = value(rules, "BEHAVIOR_WINDOW");
        return window > 0 ? window : DEFAULT_WINDOW;
    }

    /**
     * 计算行为分目标值
     *
     * @param stats 行为统计（来自 lease_order 现算）
     * @param rules 规则表按 ruleCode 索引
     * @return 行为分目标值，区间 [-FLOOR, 0]
     */
    public int target(CreditBehaviorStats stats, Map<String, CreditRule> rules) {
        int wOnTime = value(rules, "BEHAVIOR_ON_TIME");
        int wCancel = value(rules, "BEHAVIOR_CANCEL");
        int wOverdue = value(rules, "BEHAVIOR_OVERDUE");
        int floor = value(rules, "BEHAVIOR_FLOOR");

        if (wOnTime == 0 && wCancel == 0 && wOverdue == 0) {
            // 一条 BEHAVIOR_* 规则都没配，视为未启用人格化行为分
            return 0;
        }

        BigDecimal onTimeRate = stats.getFinishedCount() != null && stats.getFinishedCount() > 0
                ? rate(stats.getOnTimeCount(), stats.getFinishedCount())
                : BigDecimal.ONE;
        BigDecimal cancelRate = stats.getPaidCount() != null && stats.getPaidCount() > 0
                ? rate(stats.getPaidCancelCount(), stats.getPaidCount())
                : BigDecimal.ZERO;

        int overdueCnt = stats.getOverdueCount() == null ? 0 : stats.getOverdueCount();

        BigDecimal raw = BigDecimal.valueOf(wOnTime).multiply(onTimeRate)
                .add(BigDecimal.valueOf(wCancel).multiply(BigDecimal.ONE.subtract(cancelRate)))
                .subtract(BigDecimal.valueOf((long) wOverdue * overdueCnt))
                .subtract(BigDecimal.valueOf((long) wOnTime + wCancel));

        int target = raw.setScale(0, RoundingMode.HALF_UP).intValue();
        if (floor > 0) {
            target = Math.max(target, -floor);
        }
        return Math.min(target, 0);
    }

    /** 读取一条规则的分值，缺失返回中性值 */
    public int value(Map<String, CreditRule> rules, String ruleCode) {
        CreditRule rule = rules.get(ruleCode);
        if (rule == null || rule.getScoreValue() == null) {
            return DEFAULT_WEIGHT;
        }
        return rule.getScoreValue();
    }

    private BigDecimal rate(Integer numerator, Integer denominator) {
        if (numerator == null || denominator == null || denominator == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(numerator)
                .divide(BigDecimal.valueOf(denominator), 4, RoundingMode.HALF_UP);
    }
}
