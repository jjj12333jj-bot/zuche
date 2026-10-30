package com.zuche.module.credit.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zuche.common.BusinessException;
import com.zuche.common.PageResult;
import com.zuche.common.ResultCode;
import com.zuche.module.credit.dto.CreditEvent;
import com.zuche.module.credit.dto.CreditLogQueryDTO;
import com.zuche.module.credit.dto.CreditRuleUpdateDTO;
import com.zuche.module.credit.dto.LevelRuleUpdateDTO;
import com.zuche.module.credit.entity.CreditLog;
import com.zuche.module.credit.entity.CreditRule;
import com.zuche.module.credit.mapper.CreditLogMapper;
import com.zuche.module.credit.mapper.CreditRuleMapper;
import com.zuche.module.credit.service.CreditBehaviorEngine;
import com.zuche.module.credit.service.CreditService;
import com.zuche.module.credit.vo.CreditBehaviorStats;
import com.zuche.module.credit.vo.CreditCenterVO;
import com.zuche.module.credit.vo.CreditChangeVO;
import com.zuche.module.credit.vo.CreditLevelVO;
import com.zuche.module.credit.vo.CreditLogVO;
import com.zuche.module.credit.vo.CreditRuleVO;
import com.zuche.module.credit.vo.CreditSnapshot;
import com.zuche.module.user.entity.SysUser;
import com.zuche.module.user.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 信用服务实现
 *
 * <p>写入链路只有一条：{@link #doRecord}。所有事件（完成、按时、逾期、取消、行为重算）
 * 都走同一条路，顺序固定为「幂等预检 → 行锁用户 → 算分 → 写日志 → 回写分数与等级」，
 * 因此并发下不会出现两个人同时读到 100 分、各加 5 分却只落了 105 的情况。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CreditServiceImpl implements CreditService {

    /** 找不到等级规则时的兜底系数：不打折也不加价 */
    private static final BigDecimal DEFAULT_COEFFICIENT = BigDecimal.ONE;

    private static final String RULE_TYPE_LEVEL = "LEVEL";
    private static final String RULE_TYPE_ACTION = "ACTION";
    private static final String RULE_TYPE_PARAM = "PARAM";

    private static final String CHANGE_EVENT = "EVENT";
    private static final String CHANGE_BEHAVIOR = "BEHAVIOR";
    private static final String CHANGE_ADMIN = "ADMIN";
    private static final String CHANGE_APPEAL = "APPEAL";

    /** 基准分：新用户从 100 分起步，与注册逻辑、种子脚本保持一致 */
    private static final int BASE_SCORE = 100;

    private final SysUserMapper userMapper;
    private final CreditRuleMapper creditRuleMapper;
    private final CreditLogMapper creditLogMapper;
    private final CreditBehaviorEngine behaviorEngine;

    /* ==================== 读 ==================== */

    @Override
    public CreditSnapshot getSnapshot(Long userId) {
        SysUser user = requireUser(userId);

        CreditSnapshot snapshot = new CreditSnapshot();
        snapshot.setUserId(user.getId());
        snapshot.setCreditScore(user.getCreditScore() == null ? BASE_SCORE : user.getCreditScore());
        snapshot.setCreditLevel(user.getCreditLevel());

        CreditRule rule = creditRuleMapper.selectOne(Wrappers.<CreditRule>lambdaQuery()
                .eq(CreditRule::getRuleType, RULE_TYPE_LEVEL)
                .eq(CreditRule::getRuleCode, user.getCreditLevel())
                .eq(CreditRule::getStatus, 1)
                .last("LIMIT 1"));

        if (rule == null) {
            // 配置缺失不应阻断交易，但必须留痕，方便后台补配置
            log.warn("信用等级规则缺失: level={}, userId={}，押金系数按 1.00 处理",
                    user.getCreditLevel(), userId);
            snapshot.setLevelLabel(user.getCreditLevel());
            snapshot.setDepositCoefficient(DEFAULT_COEFFICIENT);
            return snapshot;
        }

        snapshot.setLevelLabel(rule.getRuleName());
        snapshot.setDepositCoefficient(rule.getDepositCoefficient() == null
                ? DEFAULT_COEFFICIENT : rule.getDepositCoefficient());
        snapshot.setLevelColor(rule.getColor());
        snapshot.setLevelDescription(rule.getDescription());
        return snapshot;
    }

    @Override
    public Map<String, String> levelLabelMap() {
        Map<String, String> map = new LinkedHashMap<>();
        for (CreditRule rule : levelRules()) {
            map.put(rule.getRuleCode(), rule.getRuleName());
        }
        return map;
    }

    @Override
    public List<CreditLevelVO> levelRules(String currentLevel) {
        List<CreditLevelVO> list = new ArrayList<>();
        for (CreditRule rule : levelRules()) {
            CreditLevelVO vo = new CreditLevelVO();
            vo.setCode(rule.getRuleCode());
            vo.setName(rule.getRuleName());
            vo.setMinScore(rule.getMinScore());
            vo.setMaxScore(rule.getMaxScore());
            vo.setDepositCoefficient(rule.getDepositCoefficient());
            vo.setColor(rule.getColor());
            vo.setDescription(rule.getDescription());
            vo.setCurrent(rule.getRuleCode().equals(currentLevel));
            list.add(vo);
        }
        return list;
    }

    @Override
    public CreditCenterVO getCenter(Long userId) {
        CreditSnapshot snapshot = getSnapshot(userId);
        Map<String, CreditRule> actionRules = loadActionRules();
        CreditBehaviorStats stats = loadStats(userId, behaviorEngine.window(actionRules));

        int behaviorTarget = behaviorEngine.target(stats, actionRules);
        int behaviorScore = creditLogMapper.sumBehaviorChange(userId);

        CreditCenterVO vo = new CreditCenterVO();
        vo.setUserId(userId);
        vo.setCreditScore(snapshot.getCreditScore());
        vo.setCreditLevel(snapshot.getCreditLevel());
        vo.setLevelLabel(snapshot.getLevelLabel());
        vo.setLevelColor(snapshot.getLevelColor());
        vo.setLevelDescription(snapshot.getLevelDescription());
        vo.setDepositCoefficient(snapshot.getDepositCoefficient());
        vo.setBehaviorScore(behaviorScore);
        vo.setBehaviorTarget(behaviorTarget);
        vo.setStats(stats);
        vo.setLevels(levelRules(snapshot.getCreditLevel()));

        // 距下一档还差多少分：按 min_score 升序找到第一个高于当前分的等级
        for (CreditRule rule : levelRules()) {
            if (rule.getMinScore() != null && rule.getMinScore() > snapshot.getCreditScore()) {
                vo.setNextLevelLabel(rule.getRuleName());
                vo.setScoreToNextLevel(rule.getMinScore() - snapshot.getCreditScore());
                break;
            }
        }
        return vo;
    }

    @Override
    public PageResult<CreditLogVO> pageLogs(Long userId, CreditLogQueryDTO query) {
        long current = query == null || query.getCurrent() == null || query.getCurrent() < 1
                ? 1L : query.getCurrent();
        long size = query == null || query.getPageSize() == null || query.getPageSize() < 1
                ? 10L : Math.min(query.getPageSize(), 50L);

        Page<CreditLogVO> page = new Page<>(current, size);
        String changeType = query == null || !StringUtils.hasText(query.getChangeType())
                ? null : query.getChangeType().trim().toUpperCase();

        IPage<CreditLogVO> result = creditLogMapper.selectLogPage(page, userId, changeType);
        result.getRecords().forEach(vo -> vo.setChangeTypeLabel(changeTypeLabel(vo.getChangeType())));
        return PageResult.of(result);
    }

    @Override
    public int maxTimes(String actionCode) {
        if (!StringUtils.hasText(actionCode)) {
            return 1;
        }
        String code = actionCode.trim().toUpperCase();
        CreditRule rule = loadActionRules().get(code);
        if (rule == null) {
            // 违约金单价这类参数也配了"最多计费天数"，同样放在 max_times 上
            rule = findParam(code);
        }
        if (rule == null || rule.getMaxTimes() == null || rule.getMaxTimes() < 1) {
            return 1;
        }
        return rule.getMaxTimes();
    }

    /* ==================== S4：后台规则与参数 ==================== */

    @Override
    public List<CreditRuleVO> listRules(String ruleType) {
        String type = StringUtils.hasText(ruleType) ? ruleType.trim().toUpperCase() : null;
        List<CreditRule> rules = creditRuleMapper.selectList(Wrappers.<CreditRule>lambdaQuery()
                .eq(type != null, CreditRule::getRuleType, type)
                .orderByAsc(CreditRule::getSortNo));
        List<CreditRuleVO> list = new ArrayList<>(rules.size());
        for (CreditRule rule : rules) {
            list.add(toRuleVO(rule));
        }
        return list;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CreditRuleVO updateRule(Long id, CreditRuleUpdateDTO dto) {
        CreditRule rule = creditRuleMapper.selectById(id);
        if (rule == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "规则不存在");
        }
        if (RULE_TYPE_LEVEL.equals(rule.getRuleType())) {
            throw new BusinessException(ResultCode.BAD_REQUEST,
                    "等级规则涉及区间衔接，请使用整组保存接口");
        }
        if (dto == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "没有需要修改的内容");
        }

        if (StringUtils.hasText(dto.getRuleName())) {
            rule.setRuleName(dto.getRuleName().trim());
        }
        if (dto.getScoreValue() != null) {
            if (RULE_TYPE_ACTION.equals(rule.getRuleType()) && Math.abs(dto.getScoreValue()) > 100) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "单个事件的分值不能超过 ±100");
            }
            if (RULE_TYPE_PARAM.equals(rule.getRuleType()) && dto.getScoreValue() < 0) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "风控参数不能为负值");
            }
            rule.setScoreValue(dto.getScoreValue());
        }
        if (rule.getScoreValue() == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "分值/参数值不能为空");
        }
        if (dto.getMaxTimes() != null) {
            rule.setMaxTimes(dto.getMaxTimes());
        }
        if (dto.getDescription() != null) {
            rule.setDescription(dto.getDescription().trim());
        }
        if (dto.getColor() != null) {
            rule.setColor(dto.getColor().trim());
        }
        if (dto.getStatus() != null) {
            rule.setStatus(dto.getStatus() == 1 ? 1 : 0);
        }
        rule.setUpdateTime(LocalDateTime.now());
        creditRuleMapper.updateById(rule);

        log.info("规则已更新: ruleCode={}, 分值={}, 状态={}",
                rule.getRuleCode(), rule.getScoreValue(), rule.getStatus());
        return toRuleVO(rule);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<CreditRuleVO> updateLevelRules(List<LevelRuleUpdateDTO> items) {
        if (items == null || items.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "请提交等级规则");
        }

        List<CreditRule> enabled = levelRules();
        if (enabled.size() < 2) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "等级规则配置不足，无法保存");
        }
        Map<Long, CreditRule> byId = new LinkedHashMap<>();
        for (CreditRule rule : enabled) {
            byId.put(rule.getId(), rule);
        }

        List<CreditRule> merged = new ArrayList<>();
        for (LevelRuleUpdateDTO item : items) {
            CreditRule rule = item == null || item.getId() == null ? null : byId.get(item.getId());
            if (rule == null) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "存在不属于当前等级配置的规则 ID");
            }
            if (item.getMinScore() > item.getMaxScore()) {
                throw new BusinessException(ResultCode.BAD_REQUEST,
                        "「" + rule.getRuleName() + "」的下限不能大于上限");
            }
            rule.setMinScore(item.getMinScore());
            rule.setMaxScore(item.getMaxScore());
            rule.setDepositCoefficient(item.getDepositCoefficient());
            if (StringUtils.hasText(item.getRuleName())) {
                rule.setRuleName(item.getRuleName().trim());
            }
            if (item.getColor() != null) {
                rule.setColor(item.getColor().trim());
            }
            if (item.getDescription() != null) {
                rule.setDescription(item.getDescription().trim());
            }
            merged.add(rule);
        }
        // 未提交的启用等级原样带入，保证校验的是"保存后的完整集合"
        for (CreditRule rule : enabled) {
            if (merged.stream().noneMatch(r -> r.getId().equals(rule.getId()))) {
                merged.add(rule);
            }
        }

        merged.sort(Comparator.comparing(CreditRule::getMinScore));
        if (merged.get(0).getMinScore() != 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST,
                    "最低等级必须从 0 分开始，当前从 " + merged.get(0).getMinScore() + " 分开始");
        }
        for (int i = 1; i < merged.size(); i++) {
            CreditRule prev = merged.get(i - 1);
            CreditRule cur = merged.get(i);
            if (cur.getMinScore() <= prev.getMaxScore()) {
                throw new BusinessException(ResultCode.BAD_REQUEST, String.format(
                        "「%s」上限 %d 与「%s」下限 %d 重叠，请调整区间",
                        prev.getRuleName(), prev.getMaxScore(), cur.getRuleName(), cur.getMinScore()));
            }
            if (cur.getMinScore() > prev.getMaxScore() + 1) {
                throw new BusinessException(ResultCode.BAD_REQUEST, String.format(
                        "%d~%d 分没有对应等级，区间必须首尾相接",
                        prev.getMaxScore() + 1, cur.getMinScore() - 1));
            }
        }

        LocalDateTime now = LocalDateTime.now();
        for (CreditRule rule : merged) {
            rule.setUpdateTime(now);
            creditRuleMapper.updateById(rule);
        }

        int changed = recalcAllLevels();
        log.info("等级规则已整组保存: {} 档, 受影响用户 {} 人", merged.size(), changed);
        return listRules(RULE_TYPE_LEVEL);
    }

    @Override
    public int recalcAllLevels() {
        int changed = userMapper.recalcLevels();
        if (changed > 0) {
            log.info("按最新等级区间重算信用等级，变更 {} 人", changed);
        }
        return changed;
    }

    @Override
    public int param(String code, int defaultValue) {
        if (!StringUtils.hasText(code)) {
            return defaultValue;
        }
        CreditRule rule = findParam(code.trim().toUpperCase());
        if (rule == null || rule.getScoreValue() == null) {
            log.warn("风控参数未配置或已停用: {}，使用默认值 {}", code, defaultValue);
            return defaultValue;
        }
        return rule.getScoreValue();
    }

    /** 按编码取启用中的 PARAM 规则，取不到返回 null */
    private CreditRule findParam(String code) {
        return creditRuleMapper.selectOne(Wrappers.<CreditRule>lambdaQuery()
                .eq(CreditRule::getRuleType, RULE_TYPE_PARAM)
                .eq(CreditRule::getRuleCode, code)
                .eq(CreditRule::getStatus, 1)
                .last("LIMIT 1"));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CreditChangeVO adjustByAdmin(Long userId, int scoreChange, String reason, Long operatorId) {
        if (userId == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "请选择要调整的用户");
        }
        if (scoreChange == 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "调整分值不能为 0");
        }
        if (Math.abs(scoreChange) > 50) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "单次人工调整不能超过 ±50 分");
        }
        if (!StringUtils.hasText(reason) || reason.trim().length() < 5) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "请填写至少 5 个字的调整原因");
        }

        // 已经顶到上下限时再调分只会产生一条"变动 0"的空日志，直接拒绝
        SysUser target = userMapper.selectById(userId);
        if (target == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }
        int[] bounds = scoreBounds();
        int current = target.getCreditScore() == null ? BASE_SCORE : target.getCreditScore();
        if (scoreChange > 0 && current >= bounds[1]) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR,
                    "该用户当前 " + current + " 分，已是上限（" + bounds[1] + " 分），无法继续加分");
        }
        if (scoreChange < 0 && current <= bounds[0]) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR,
                    "该用户当前 " + current + " 分，已到下限（" + bounds[0] + " 分），无法继续扣分");
        }

        CreditEvent event = new CreditEvent();
        event.setUserId(userId);
        event.setActionCode("ADMIN_ADJUST");
        event.setScoreChange(scoreChange);
        // 人工调整没有订单号，唯一索引的 order_id 维为空，必须自带单调幂等键
        event.setDedupKey("admin-adj-" + System.currentTimeMillis()
                + "-" + ThreadLocalRandom.current().nextInt(1000, 10000));
        event.setRemark("管理员人工调整：" + reason.trim());
        event.setOperatorId(operatorId);
        return doRecord(event, CHANGE_ADMIN);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CreditChangeVO restoreByAppeal(Long userId, Long orderId, int scoreChange,
                                          String reason, Long appealId, Long operatorId) {
        if (appealId == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "申诉单不存在");
        }
        if (scoreChange <= 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "补回分值必须大于 0");
        }

        CreditEvent event = new CreditEvent();
        event.setUserId(userId);
        event.setOrderId(orderId);
        event.setActionCode("APPEAL_RESTORE");
        event.setScoreChange(scoreChange);
        event.setDedupKey("appeal-" + appealId);
        event.setRemark(StringUtils.hasText(reason) ? reason.trim() : "信用申诉通过，补回被扣分数");
        event.setOperatorId(operatorId);
        return doRecord(event, CHANGE_APPEAL);
    }

    /* ==================== 写 ==================== */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CreditChangeVO recordEvent(CreditEvent event) {
        return doRecord(event, CHANGE_EVENT);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CreditChangeVO refitBehavior(Long userId, Long orderId) {
        Map<String, CreditRule> actionRules = loadActionRules();
        CreditBehaviorStats stats = loadStats(userId, behaviorEngine.window(actionRules));

        int target = behaviorEngine.target(stats, actionRules);
        int applied = creditLogMapper.sumBehaviorChange(userId);
        int delta = target - applied;

        if (delta == 0) {
            // 行为特征没变（或本来就停在边界），不产生日志，避免明细被 0 分记录刷屏
            return null;
        }

        CreditEvent adjust = new CreditEvent();
        adjust.setUserId(userId);
        adjust.setOrderId(orderId);
        adjust.setActionCode("BEHAVIOR_ADJUST");
        adjust.setDedupKey(orderId == null
                ? "beh-manual-" + System.nanoTime()
                : "beh-" + orderId);
        adjust.setScoreChange(delta);
        adjust.setRemark(String.format("行为评分校准：按时率 %s%%，取消率 %s%%，逾期 %d 单",
                percent(stats.getOnTimeRate()), percent(stats.getCancelRate()),
                stats.getOverdueCount() == null ? 0 : stats.getOverdueCount()));

        log.info("行为分重算: userId={}, orderId={}, target={}, applied={}, delta={}",
                userId, orderId, target, applied, delta);
        return doRecord(adjust, CHANGE_BEHAVIOR);
    }

    /**
     * 唯一的写入口
     *
     * <p>为什么分数要按 {@code clamp(100 + Σ, 0, 150)} 重算，而不是简单的 before + change：
     * 一旦某次加分把分数顶到 150，之后再来一笔扣分，"累加值"和"实际显示值"就会分叉。
     * 以日志总和为准重算，恒等式在任何加减组合下都成立，也能顺手修掉历史漂移。</p>
     */
    private CreditChangeVO doRecord(CreditEvent event, String changeType) {
        if (event == null || event.getUserId() == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "信用事件缺少用户");
        }
        if (!StringUtils.hasText(event.getActionCode())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "信用事件缺少行为码");
        }

        Long userId = event.getUserId();
        String actionCode = event.getActionCode().trim().toUpperCase();
        String dedupKey = normalizeDedupKey(event);
        CreditRule rule = loadActionRules().get(actionCode);
        int nominal = resolveScore(event, actionCode, rule);

        // 1) 幂等预检：重复事件直接返回，不抛异常，避免"重试即失败"
        if (creditLogMapper.countByDedup(userId, event.getOrderId(), actionCode, dedupKey) > 0) {
            log.info("信用事件重复，已忽略: userId={}, action={}, orderId={}, dedup={}",
                    userId, actionCode, event.getOrderId(), dedupKey);
            return duplicated(actionCode, rule);
        }

        // 2) 行锁用户：同一用户的并发加减分在这里串行化
        SysUser user = userMapper.selectByIdForUpdate(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }
        int before = user.getCreditScore() == null ? BASE_SCORE : user.getCreditScore();
        int sum = creditLogMapper.sumAllChange(userId);

        int[] bounds = scoreBounds();
        int after = Math.min(bounds[1], Math.max(bounds[0], BASE_SCORE + sum + nominal));
        int delta = after - before;

        // 3) 写日志（唯一索引是最后一道幂等闸门）
        LocalDateTime now = LocalDateTime.now();
        CreditLog logRow = new CreditLog();
        logRow.setUserId(userId);
        logRow.setActionCode(actionCode);
        logRow.setChangeType(changeType);
        logRow.setScoreBefore(before);
        logRow.setScoreChange(delta);
        logRow.setScoreAfter(after);
        logRow.setOrderId(event.getOrderId());
        logRow.setDedupKey(dedupKey);
        logRow.setReason(buildReason(event, nominal, delta));
        logRow.setOperatorId(event.getOperatorId());
        logRow.setCreateTime(now);
        logRow.setUpdateTime(now);
        try {
            creditLogMapper.insert(logRow);
        } catch (DuplicateKeyException e) {
            log.info("信用事件命中唯一索引，已忽略: userId={}, action={}, dedup={}", userId, actionCode, dedupKey);
            return duplicated(actionCode, rule);
        }

        // 4) 回写分数与等级：等级永远由分数反查 credit_rule，不单独维护
        String levelBefore = user.getCreditLevel();
        String levelAfter = resolveLevel(after, levelBefore);
        userMapper.updateCredit(userId, after, levelAfter);

        log.info("信用变动: userId={}, action={}, {}→{}({}), 等级 {}→{}",
                userId, actionCode, before, after, delta, levelBefore, levelAfter);

        CreditChangeVO vo = new CreditChangeVO();
        vo.setActionCode(actionCode);
        vo.setActionName(rule == null ? actionCode : rule.getRuleName());
        vo.setScoreBefore(before);
        vo.setScoreChange(delta);
        vo.setScoreAfter(after);
        vo.setLevelBefore(levelBefore);
        vo.setLevelAfter(levelAfter);
        vo.setLevelChanged(!java.util.Objects.equals(levelBefore, levelAfter));
        vo.setDuplicated(false);
        if (vo.getLevelChanged()) {
            vo.setLevelAfterLabel(levelLabelMap().get(levelAfter));
        }
        return vo;
    }

    /* ==================== 内部方法 ==================== */

    /**
     * 幂等键归一化
     *
     * <p>有订单的事件用空串即可：唯一索引是 (order_id, action_code, dedup_key)，
     * "同一订单同一事件只记一次"由 order_id 这一维保证，逾期按天扣才需要额外传 day-N。</p>
     *
     * <p>没有订单的事件（人工调整）必须自己造键：MySQL 的唯一索引对 NULL 不生效，
     * order_id 为 NULL 时插多少条都不会冲突，等于没有幂等保护。所以这里补一个单调键，
     * 让每次人工调整在库里是可区分、可追溯的独立记录。</p>
     */
    private String normalizeDedupKey(CreditEvent event) {
        if (StringUtils.hasText(event.getDedupKey())) {
            return event.getDedupKey().trim();
        }
        if (event.getOrderId() == null) {
            return "manual-" + System.nanoTime();
        }
        return "";
    }

    private int resolveScore(CreditEvent event, String actionCode, CreditRule rule) {
        if (event.getScoreChange() != null) {
            return event.getScoreChange();
        }
        if (rule != null && rule.getScoreValue() != null) {
            return rule.getScoreValue();
        }
        // 规则缺失必须报错：宁可这笔业务失败，也不能悄悄按 0 分处理
        throw new BusinessException(ResultCode.BUSINESS_ERROR,
                "信用规则未配置或已停用：" + actionCode);
    }

    private String buildReason(CreditEvent event, int nominal, int delta) {
        StringBuilder reason = new StringBuilder();
        if (StringUtils.hasText(event.getRemark())) {
            reason.append(event.getRemark());
        }
        if (delta != nominal) {
            reason.append(reason.length() > 0 ? "；" : "")
                    .append("规则分值 ").append(nominal).append(" 分，因触达分数")
                    .append(nominal > 0 ? "上限" : "下限").append("实际计入 ").append(delta).append(" 分");
        }
        return reason.length() == 0 ? null : reason.toString();
    }

    private CreditChangeVO duplicated(String actionCode, CreditRule rule) {
        CreditChangeVO vo = new CreditChangeVO();
        vo.setActionCode(actionCode);
        vo.setActionName(rule == null ? actionCode : rule.getRuleName());
        vo.setScoreChange(0);
        vo.setDuplicated(true);
        return vo;
    }

    /** 用分数反查等级：区间由 credit_rule 的 LEVEL 规则定义，Java 里没有阈值常量 */
    private String resolveLevel(int score, String fallback) {
        for (CreditRule rule : levelRules()) {
            if (rule.getMinScore() == null || rule.getMaxScore() == null) {
                continue;
            }
            if (score >= rule.getMinScore() && score <= rule.getMaxScore()) {
                return rule.getRuleCode();
            }
        }
        return fallback;
    }

    /** 分数上下限同样来自等级规则覆盖的区间，不写死 0/150 */
    private int[] scoreBounds() {
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        for (CreditRule rule : levelRules()) {
            if (rule.getMinScore() != null) {
                min = Math.min(min, rule.getMinScore());
            }
            if (rule.getMaxScore() != null) {
                max = Math.max(max, rule.getMaxScore());
            }
        }
        if (min == Integer.MAX_VALUE || max == Integer.MIN_VALUE) {
            return new int[]{0, 150};
        }
        return new int[]{min, max};
    }

    private List<CreditRule> levelRules() {
        return creditRuleMapper.selectList(Wrappers.<CreditRule>lambdaQuery()
                .eq(CreditRule::getRuleType, RULE_TYPE_LEVEL)
                .eq(CreditRule::getStatus, 1)
                .orderByAsc(CreditRule::getSortNo));
    }

    private Map<String, CreditRule> loadActionRules() {
        List<CreditRule> rules = creditRuleMapper.selectList(Wrappers.<CreditRule>lambdaQuery()
                .eq(CreditRule::getRuleType, RULE_TYPE_ACTION)
                .eq(CreditRule::getStatus, 1));
        Map<String, CreditRule> map = new LinkedHashMap<>(rules.size());
        for (CreditRule rule : rules) {
            map.put(rule.getRuleCode(), rule);
        }
        return map;
    }

    private SysUser requireUser(Long userId) {
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }
        return user;
    }

    /** 取行为统计（滚动窗口）并补上比率字段 */
    private CreditBehaviorStats loadStats(Long userId, int window) {
        CreditBehaviorStats stats = creditLogMapper.selectBehaviorStats(userId, window);
        if (stats == null) {
            stats = new CreditBehaviorStats();
        }
        stats.setWindowSize(window);
        fillRates(stats);
        return stats;
    }

    private void fillRates(CreditBehaviorStats stats) {
        int finished = stats.getFinishedCount() == null ? 0 : stats.getFinishedCount();
        int onTime = stats.getOnTimeCount() == null ? 0 : stats.getOnTimeCount();
        int paid = stats.getPaidCount() == null ? 0 : stats.getPaidCount();
        int cancelled = stats.getPaidCancelCount() == null ? 0 : stats.getPaidCancelCount();

        // 没有样本时按"满分"展示：新用户不该一进信用中心就看到 0% 按时率
        stats.setOnTimeRate(finished == 0
                ? BigDecimal.ONE.setScale(4, RoundingMode.HALF_UP)
                : BigDecimal.valueOf(onTime).divide(BigDecimal.valueOf(finished), 4, RoundingMode.HALF_UP));
        stats.setCancelRate(paid == 0
                ? BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP)
                : BigDecimal.valueOf(cancelled).divide(BigDecimal.valueOf(paid), 4, RoundingMode.HALF_UP));
    }

    private String percent(BigDecimal rate) {
        if (rate == null) {
            return "0";
        }
        return rate.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).toPlainString();
    }

    private CreditRuleVO toRuleVO(CreditRule rule) {
        CreditRuleVO vo = new CreditRuleVO();
        BeanUtils.copyProperties(rule, vo);
        vo.setRuleTypeLabel(ruleTypeLabel(rule.getRuleType()));
        return vo;
    }

    private String ruleTypeLabel(String ruleType) {
        if (ruleType == null) {
            return "未知";
        }
        return switch (ruleType) {
            case RULE_TYPE_LEVEL -> "等级规则";
            case RULE_TYPE_ACTION -> "事件规则";
            case RULE_TYPE_PARAM -> "风控参数";
            default -> ruleType;
        };
    }

    private String changeTypeLabel(String changeType) {
        if (changeType == null) {
            return "业务事件";
        }
        return switch (changeType) {
            case "BEHAVIOR" -> "行为评分";
            case "ADMIN" -> "人工调整";
            case "APPEAL" -> "申诉复核";
            default -> "业务事件";
        };
    }
}
