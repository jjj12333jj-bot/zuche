package com.zuche.module.credit.service;

import com.zuche.common.PageResult;
import com.zuche.module.credit.dto.CreditEvent;
import com.zuche.module.credit.dto.CreditLogQueryDTO;
import com.zuche.module.credit.dto.CreditRuleUpdateDTO;
import com.zuche.module.credit.dto.LevelRuleUpdateDTO;
import com.zuche.module.credit.vo.CreditCenterVO;
import com.zuche.module.credit.vo.CreditChangeVO;
import com.zuche.module.credit.vo.CreditLevelVO;
import com.zuche.module.credit.vo.CreditLogVO;
import com.zuche.module.credit.vo.CreditRuleVO;
import com.zuche.module.credit.vo.CreditSnapshot;

import java.util.List;
import java.util.Map;

/**
 * 信用风控服务
 *
 * <p>设计意图：全系统唯一改信用分的入口。任何业务（支付、取消、归还、后台调整）
 * 都不允许直接 update sys_user.credit_score，只能调用本服务，由它在同一事务内
 * 「写 credit_log + 回写分数 + 重算等级」三件事一起做，保证：</p>
 *
 * <pre>credit_score == clamp(100 + Σ credit_log.score_change, 0, 150)</pre>
 *
 * <p>这条恒等式是答辩时的核心论据：分数永远可由日志重算出来，不存在"分数被谁改过查不到"。</p>
 */
public interface CreditService {

    /**
     * 取用户当前信用快照（分数 / 等级 / 押金系数）
     *
     * <p>押金系数不做硬编码，全部来自 credit_rule 的 LEVEL 规则，
     * 后台改了阈值或系数，下单页立刻生效。</p>
     */
    CreditSnapshot getSnapshot(Long userId);

    /**
     * 等级码 → 等级名称的映射（EXCELLENT→优秀）
     *
     * <p>列表页要显示历史订单的等级快照，名称来自 credit_rule 而不是硬编码，
     * 后台把"优秀"改成别的叫法时，历史订单的展示也跟着统一。</p>
     */
    Map<String, String> levelLabelMap();

    /**
     * 记录一次信用事件并回写分数（幂等）
     *
     * <p>实现约束：</p>
     * <ol>
     *   <li>同一事务内：insert credit_log → update sys_user（分数 + 等级）</li>
     *   <li>insert 依赖唯一索引 (order_id, action_code, dedup_key)，重复即视为已处理，
     *       返回 {@code duplicated=true} 而不是抛异常——重复调用不应该让业务失败</li>
     *   <li>分值取 credit_rule.score_value；只有调用方显式传 scoreChange 时才覆盖</li>
     * </ol>
     *
     * @return 本次变动结果；被幂等拦下时 scoreChange = 0、duplicated = true
     */
    CreditChangeVO recordEvent(CreditEvent event);

    /**
     * 按行为特征重算行为分，并把差额写进 credit_log
     *
     * <p>行为分与事件分分开记：事件分记"这一单发生了什么"，
     * 行为分记"这个人的长期比率特征折算成多少分"。
     * 重算是幂等的——同一订单只会产生一条 BEHAVIOR 日志（dedup_key = beh-订单号），
     * 重跑只会得到差额 0。</p>
     *
     * @param userId  用户
     * @param orderId 触发本次重算的订单（用于幂等键与明细溯源）
     */
    CreditChangeVO refitBehavior(Long userId, Long orderId);

    /** 信用中心概览：当前状态 + 行为统计 + 全部等级规则 */
    CreditCenterVO getCenter(Long userId);

    /** 信用明细分页 */
    PageResult<CreditLogVO> pageLogs(Long userId, CreditLogQueryDTO query);

    /** 全部等级规则，current 标记当前所处等级 */
    List<CreditLevelVO> levelRules(String currentLevel);

    /**
     * 某行为规则在单笔订单上最多生效几次（credit_rule.max_times）
     *
     * <p>逾期扣分要按天累加，但不能无限扣：规则里配了"单订单最多扣 7 天"，
     * 这个上限必须从配置读，业务层不能写死循环次数。</p>
     */
    int maxTimes(String actionCode);

    /* ==================== S4：后台风控与参数 ==================== */

    /**
     * 后台规则列表（含停用项）
     *
     * @param ruleType LEVEL / ACTION / PARAM，为空查全部
     */
    List<CreditRuleVO> listRules(String ruleType);

    /**
     * 修改规则值并立即生效
     *
     * <p>等级规则改完会触发一次全量等级重算：阈值从 120 调到 125 后，
     * 原来 120~124 分的用户必须当场掉到下一档，否则"后台改完页面没反应"，
     * 押金系数仍然按旧等级算，等于配置没生效。</p>
     */
    CreditRuleVO updateRule(Long id, CreditRuleUpdateDTO dto);

    /**
     * 整组保存等级规则（四档必须首尾相接、从 0 分起）
     *
     * <p>单行保存会产生"120~124 分没有等级"的中间态，因此等级配置只允许整组提交。</p>
     */
    List<CreditRuleVO> updateLevelRules(List<LevelRuleUpdateDTO> rules);

    /** 按当前 LEVEL 规则重算全部用户的信用等级，返回实际变更人数 */
    int recalcAllLevels();

    /**
     * 读取 PARAM 类型参数
     *
     * <p>违约金单价、预警天数、拉黑阈值都从这里取；规则缺失时用调用方给的默认值，
     * 保证配置漏了也不会让业务崩掉。</p>
     */
    int param(String code, int defaultValue);

    /**
     * 管理员人工调分（必填原因）
     *
     * <p>走统一的写入口，落 {@code change_type = ADMIN} 的日志，
     * 幂等键 {@code admin-adj-时间戳-随机} 保证每次调整都是独立可追溯的一条记录。</p>
     */
    CreditChangeVO adjustByAdmin(Long userId, int scoreChange, String reason, Long operatorId);

    /**
     * 申诉通过补分
     *
     * <p>落 {@code change_type = APPEAL} 的日志，幂等键 {@code appeal-申诉单ID}，
     * 同一张申诉单重复审批也只会补一次分。</p>
     */
    CreditChangeVO restoreByAppeal(Long userId, Long orderId, int scoreChange,
                                   String reason, Long appealId, Long operatorId);
}
