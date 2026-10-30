package com.zuche.module.risk.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zuche.common.BusinessException;
import com.zuche.common.PageResult;
import com.zuche.common.ResultCode;
import com.zuche.module.credit.entity.CreditLog;
import com.zuche.module.credit.mapper.CreditLogMapper;
import com.zuche.module.credit.service.CreditService;
import com.zuche.module.credit.vo.CreditChangeVO;
import com.zuche.module.order.entity.LeaseOrder;
import com.zuche.module.order.mapper.LeaseOrderMapper;
import com.zuche.module.risk.dto.AdminUserQueryDTO;
import com.zuche.module.risk.dto.AppealQueryDTO;
import com.zuche.module.risk.dto.BlacklistAddDTO;
import com.zuche.module.risk.dto.BlacklistQueryDTO;
import com.zuche.module.risk.dto.CreditAppealCreateDTO;
import com.zuche.module.risk.dto.CreditAppealHandleDTO;
import com.zuche.module.risk.entity.CreditAppeal;
import com.zuche.module.risk.entity.CreditBlacklist;
import com.zuche.module.risk.mapper.CreditAppealMapper;
import com.zuche.module.risk.mapper.CreditBlacklistMapper;
import com.zuche.module.risk.vo.AdminUserCreditVO;
import com.zuche.module.risk.vo.CreditAppealVO;
import com.zuche.module.risk.vo.CreditBlacklistVO;
import com.zuche.module.risk.vo.OverdueScanVO;
import com.zuche.module.user.entity.SysUser;
import com.zuche.module.user.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 风控服务：逾期扫描、名单管理、申诉流转
 *
 * <p>三件事放在同一个服务里，因为它们共享同一套"风险判定"口径：
 * 逾期多少天算预警、多少天算黑名单、分数低到什么程度要拉黑，
 * 全部来自 {@code credit_rule} 的 PARAM 规则，Java 里没有任何阈值常量。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RiskControlService {

    /** 参数缺失时的兜底值，仅用于"配置表被误删"的极端情况，正常都走数据库 */
    private static final int DEFAULT_WARN_DAYS = 3;
    private static final int DEFAULT_BLACKLIST_DAYS = 7;
    private static final int DEFAULT_BLACKLIST_SCORE = 60;
    private static final int DEFAULT_EXPIRE_DAYS = 90;

    private static final String TYPE_WARNING = "WARNING";
    private static final String TYPE_OVERRIDE = "OVERRIDE";
    private static final String TYPE_BLACKLIST = "BLACKLIST";

    private static final DateTimeFormatter NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final LeaseOrderMapper orderMapper;
    private final SysUserMapper userMapper;
    private final CreditLogMapper creditLogMapper;
    private final CreditService creditService;
    private final CreditAppealMapper appealMapper;
    private final CreditBlacklistMapper blacklistMapper;
    private final TransactionTemplate transactionTemplate;

    /* ==================== 1. 下单风控拦截 ==================== */

    /**
     * 下单前风控校验
     *
     * <p>只有 {@code OVERRIDE} / {@code BLACKLIST} 会拦单，{@code WARNING} 只是记录，
     * 不能因为一次逾期就让用户完全无法用车——风控要分级，不能一刀切。</p>
     */
    public void assertCanOrder(Long userId) {
        String type = blacklistMapper.selectActiveType(userId);
        if (TYPE_OVERRIDE.equals(type) || TYPE_BLACKLIST.equals(type)) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR,
                    "当前账号已被列入风控名单（" + typeLabel(type) + "），暂无法下单，请联系客服或提交信用申诉");
        }
    }

    /* ==================== 2. 逾期扫描 ==================== */

    /**
     * 扫描逾期订单并执行风控动作
     *
     * <p>由定时任务与后台"立即扫描"按钮共用。逐单独立事务：
     * 某一单因为并发归还而失败，不影响其余订单继续处理。</p>
     */
    public OverdueScanVO scanOverdue() {
        OverdueScanVO vo = new OverdueScanVO();
        vo.setScanTime(LocalDateTime.now());

        // 先处理到期名单，避免"过期了还在拦单"
        vo.setReleasedCount(blacklistMapper.releaseExpired());

        LocalDate today = LocalDate.now();
        List<LeaseOrder> candidates = orderMapper.selectOverdueCandidates(today);
        vo.setCandidateCount(candidates.size());

        for (LeaseOrder order : candidates) {
            int days = calcOverdueDays(order.getEndDate(), today);
            if (days <= 0) {
                continue;
            }
            Boolean marked = transactionTemplate.execute(status -> markOne(order, days));
            if (!Boolean.TRUE.equals(marked)) {
                continue;
            }
            if (!"OVERDUE".equals(order.getStatus())) {
                vo.setMarkedCount(vo.getMarkedCount() + 1);
                vo.getDetails().add(String.format("%s 置为逾期（超期 %d 天）", order.getOrderNo(), days));
            }
            assessAndRecord(order.getUserId(), days, vo);
        }

        // 低分拉黑与逾期无关，单独扫一遍
        scanLowScoreUsers(vo);

        log.info("逾期扫描完成: 候选 {} 单, 新逾期 {} 单, 新增预警 {}, 新增黑名单 {}, 解除 {}",
                vo.getCandidateCount(), vo.getMarkedCount(), vo.getWarnedCount(),
                vo.getBlacklistedCount(), vo.getReleasedCount());
        return vo;
    }

    /** 单笔订单的逾期落库；返回 false 表示订单状态已被别的操作改掉，本次跳过 */
    private Boolean markOne(LeaseOrder order, int days) {
        if ("OVERDUE".equals(order.getStatus())) {
            // 已经逾期：只刷新天数，不再重复置状态
            orderMapper.refreshOverdueDays(order.getId(), days);
            return true;
        }
        int rows = orderMapper.markOverdue(order.getId(), order.getStatus(), days);
        if (rows == 0) {
            log.info("订单状态已变化，跳过逾期标记: orderNo={}", order.getOrderNo());
            return false;
        }
        return true;
    }

    /** 按逾期天数判定预警/黑名单 */
    private void assessAndRecord(Long userId, int overdueDays, OverdueScanVO vo) {
        int warnDays = creditService.param("WARN_OVERDUE_DAYS", DEFAULT_WARN_DAYS);
        int blackDays = creditService.param("BLACKLIST_OVERDUE_DAYS", DEFAULT_BLACKLIST_DAYS);

        if (blackDays > 0 && overdueDays >= blackDays) {
            if (addRisk(userId, TYPE_BLACKLIST,
                    "逾期 " + overdueDays + " 天未归还",
                    "连续逾期 ≥ " + blackDays + " 天")) {
                vo.setBlacklistedCount(vo.getBlacklistedCount() + 1);
                vo.getDetails().add("用户 " + userId + " 逾期 " + overdueDays + " 天，列入黑名单");
            }
            return;
        }
        if (warnDays > 0 && overdueDays >= warnDays) {
            if (addRisk(userId, TYPE_WARNING,
                    "逾期 " + overdueDays + " 天未归还",
                    "逾期 ≥ " + warnDays + " 天触发预警")) {
                vo.setWarnedCount(vo.getWarnedCount() + 1);
                vo.getDetails().add("用户 " + userId + " 逾期 " + overdueDays + " 天，写入预警");
            }
        }
    }

    /** 信用分低于阈值直接拉黑；阈值 <= 0 视为关闭该规则 */
    private void scanLowScoreUsers(OverdueScanVO vo) {
        int threshold = creditService.param("BLACKLIST_SCORE", DEFAULT_BLACKLIST_SCORE);
        if (threshold <= 0) {
            return;
        }
        List<SysUser> lowScoreUsers = userMapper.selectList(Wrappers.<SysUser>lambdaQuery()
                .lt(SysUser::getCreditScore, threshold)
                .eq(SysUser::getStatus, 1));
        for (SysUser user : lowScoreUsers) {
            if (!"USER".equals(userMapper.selectRoleCode(user.getId()))) {
                continue;
            }
            if (addRisk(user.getId(), TYPE_BLACKLIST,
                    "信用分 " + user.getCreditScore() + " 低于阈值 " + threshold,
                    "信用分 < " + threshold)) {
                vo.setBlacklistedCount(vo.getBlacklistedCount() + 1);
                vo.getDetails().add("用户 " + user.getUsername() + " 信用分 "
                        + user.getCreditScore() + "，列入黑名单");
            }
        }
    }

    /**
     * 写入一条风控记录（同类型已生效则跳过，保证定时任务可反复执行）
     *
     * @return true 表示本次真的新增了一条
     */
    private boolean addRisk(Long userId, String listType, String reason, String triggerRule) {
        if (blacklistMapper.countActiveType(userId, listType) > 0) {
            return false;
        }
        // 已有更高强度的记录就没必要再加一条弱的
        String active = blacklistMapper.selectActiveType(userId);
        if (TYPE_BLACKLIST.equals(active) && !TYPE_BLACKLIST.equals(listType)) {
            return false;
        }

        int expireDays = creditService.param("BLACKLIST_EXPIRE_DAYS", DEFAULT_EXPIRE_DAYS);
        LocalDateTime now = LocalDateTime.now();

        CreditBlacklist row = new CreditBlacklist();
        row.setUserId(userId);
        row.setListType(listType);
        row.setReason(reason);
        row.setTriggerRule(triggerRule);
        row.setEffectiveTime(now);
        row.setExpireTime(expireDays > 0 ? now.plusDays(expireDays) : null);
        row.setStatus("ACTIVE");
        row.setCreateTime(now);
        row.setUpdateTime(now);
        blacklistMapper.insert(row);

        log.info("风控名单新增: userId={}, type={}, reason={}", userId, listType, reason);
        return true;
    }

    /* ==================== 3. 申诉 ==================== */

    /** 用户提交申诉：只能对自己真实的扣分记录发起 */
    @Transactional(rollbackFor = Exception.class)
    public CreditAppealVO createAppeal(Long userId, CreditAppealCreateDTO dto) {
        CreditLog logRow = creditLogMapper.selectById(dto.getCreditLogId());
        if (logRow == null || !logRow.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "扣分记录不存在");
        }
        if (logRow.getScoreChange() == null || logRow.getScoreChange() >= 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "只能对扣分记录发起申诉");
        }
        // 行为评分是按按时率/取消率/逾期次数整体折算出来的，没有"单条事实"可复核，
        // 允许申诉只会产生无法判定的工单；要申诉的是具体事件（逾期、取消）
        if ("BEHAVIOR".equals(logRow.getChangeType())) {
            throw new BusinessException(ResultCode.BAD_REQUEST,
                    "行为评分由模型按履约数据自动折算，不支持单独申诉；如对某笔订单有异议请申诉对应事件");
        }
        if (appealMapper.countPending(dto.getCreditLogId(), userId) > 0) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "该扣分记录已有待处理的申诉，请耐心等待");
        }

        LocalDateTime now = LocalDateTime.now();
        CreditAppeal appeal = new CreditAppeal();
        appeal.setAppealNo(generateAppealNo());
        appeal.setUserId(userId);
        appeal.setCreditLogId(logRow.getId());
        appeal.setOrderId(logRow.getOrderId());
        appeal.setAppealReason(dto.getReason().trim());
        appeal.setStatus("PENDING");
        appeal.setRestoreScore(0);
        appeal.setCreateTime(now);
        appeal.setUpdateTime(now);
        appealMapper.insert(appeal);

        log.info("信用申诉提交: appealNo={}, userId={}, logId={}", appeal.getAppealNo(), userId, logRow.getId());
        return fillAppealLabels(appealMapper.selectAppealVO(appeal.getId()));
    }

    /** 用户查看自己的申诉 */
    public PageResult<CreditAppealVO> myAppeals(Long userId, AppealQueryDTO query) {
        return pageAppeals(query, userId);
    }

    public PageResult<CreditAppealVO> pageAppeals(AppealQueryDTO query) {
        return pageAppeals(query, null);
    }

    /**
     * 管理员审批
     *
     * <p>通过时补分走 {@code CreditService.restoreByAppeal}，与业务扣分共用同一个写入口，
     * 因此"分数 = 100 + Σ日志"的恒等式在申诉场景下依然成立。</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public CreditAppealVO handleAppeal(Long appealId, CreditAppealHandleDTO dto, Long operatorId) {
        CreditAppeal appeal = appealMapper.selectById(appealId);
        if (appeal == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "申诉单不存在");
        }
        if (!"PENDING".equals(appeal.getStatus())) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "该申诉已处理，请勿重复操作");
        }
        if (dto == null || dto.getApproved() == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "请选择通过或驳回");
        }

        LocalDateTime now = LocalDateTime.now();
        CreditAppeal update = new CreditAppeal();
        update.setId(appealId);
        update.setHandleUserId(operatorId);
        update.setHandleTime(now);
        update.setUpdateTime(now);

        if (dto.getApproved()) {
            int restore = resolveRestoreScore(appeal, dto);
            CreditChangeVO change = creditService.restoreByAppeal(
                    appeal.getUserId(), appeal.getOrderId(), restore,
                    "申诉 " + appeal.getAppealNo() + " 复核通过，补回 " + restore + " 分", appealId, operatorId);
            update.setStatus("APPROVED");
            update.setRestoreScore(restore);
            update.setHandleRemark(StringUtils.hasText(dto.getRemark())
                    ? dto.getRemark().trim()
                    : "申诉通过，补回 " + restore + " 分");
            log.info("申诉通过: appealNo={}, 补分={}, 分数 {}→{}", appeal.getAppealNo(), restore,
                    change == null ? null : change.getScoreBefore(),
                    change == null ? null : change.getScoreAfter());
        } else {
            update.setStatus("REJECTED");
            update.setRestoreScore(0);
            update.setHandleRemark(StringUtils.hasText(dto.getRemark())
                    ? dto.getRemark().trim() : "申诉驳回，维持原判");
            log.info("申诉驳回: appealNo={}, 意见={}", appeal.getAppealNo(), update.getHandleRemark());
        }
        appealMapper.updateById(update);

        return fillAppealLabels(appealMapper.selectAppealVO(appealId));
    }

    /** 补分默认取原扣分的绝对值，管理员可显式指定更小的值 */
    private int resolveRestoreScore(CreditAppeal appeal, CreditAppealHandleDTO dto) {
        if (dto.getRestoreScore() != null) {
            return dto.getRestoreScore();
        }
        CreditLog logRow = appeal.getCreditLogId() == null ? null
                : creditLogMapper.selectById(appeal.getCreditLogId());
        if (logRow == null || logRow.getScoreChange() == null || logRow.getScoreChange() >= 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST,
                    "原扣分记录已不可用，请手工填写补回分值");
        }
        return Math.min(Math.abs(logRow.getScoreChange()), 50);
    }

    /* ==================== 4. 名单管理 ==================== */

    public PageResult<CreditBlacklistVO> pageBlacklist(BlacklistQueryDTO query) {
        long current = query == null || query.getCurrent() == null || query.getCurrent() < 1
                ? 1L : query.getCurrent();
        long size = query == null || query.getPageSize() == null || query.getPageSize() < 1
                ? 10L : Math.min(query.getPageSize(), 50L);

        Page<CreditBlacklistVO> page = new Page<>(current, size);
        IPage<CreditBlacklistVO> result = blacklistMapper.selectBlacklistPage(page,
                upper(query == null ? null : query.getStatus()),
                upper(query == null ? null : query.getListType()),
                trim(query == null ? null : query.getKeyword()));
        result.getRecords().forEach(this::fillBlacklistLabels);
        return PageResult.of(result);
    }

    /** 手工加入名单（管理员） */
    @Transactional(rollbackFor = Exception.class)
    public void addBlacklist(BlacklistAddDTO dto, Long operatorId) {
        SysUser user = userMapper.selectById(dto.getUserId());
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }
        String type = StringUtils.hasText(dto.getListType())
                ? dto.getListType().trim().toUpperCase() : TYPE_WARNING;
        if (!TYPE_WARNING.equals(type) && !TYPE_OVERRIDE.equals(type) && !TYPE_BLACKLIST.equals(type)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "不支持的风控类型：" + dto.getListType());
        }
        if (blacklistMapper.countActiveType(dto.getUserId(), type) > 0) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR,
                    "该用户已有生效中的「" + typeLabel(type) + "」记录");
        }

        LocalDateTime now = LocalDateTime.now();
        CreditBlacklist row = new CreditBlacklist();
        row.setUserId(dto.getUserId());
        row.setListType(type);
        row.setReason(dto.getReason().trim());
        row.setTriggerRule("管理员手工加入");
        row.setEffectiveTime(now);
        row.setExpireTime(dto.getExpireDays() == null ? null : now.plusDays(dto.getExpireDays()));
        row.setStatus("ACTIVE");
        row.setOperatorId(operatorId);
        row.setCreateTime(now);
        row.setUpdateTime(now);
        blacklistMapper.insert(row);

        log.info("手工加入风控名单: userId={}, type={}, operator={}", dto.getUserId(), type, operatorId);
    }

    /** 解除名单 */
    @Transactional(rollbackFor = Exception.class)
    public void releaseBlacklist(Long id, Long operatorId) {
        CreditBlacklist row = blacklistMapper.selectById(id);
        if (row == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "名单记录不存在");
        }
        if (!"ACTIVE".equals(row.getStatus())) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "该记录已解除，请勿重复操作");
        }
        CreditBlacklist update = new CreditBlacklist();
        update.setId(id);
        update.setStatus("RELEASED");
        update.setOperatorId(operatorId);
        update.setUpdateTime(LocalDateTime.now());
        blacklistMapper.updateById(update);
        log.info("解除风控名单: id={}, userId={}, operator={}", id, row.getUserId(), operatorId);
    }

    /* ==================== 5. 后台查询 ==================== */

    public PageResult<AdminUserCreditVO> pageUsers(AdminUserQueryDTO query) {
        long current = query == null || query.getCurrent() == null || query.getCurrent() < 1
                ? 1L : query.getCurrent();
        long size = query == null || query.getPageSize() == null || query.getPageSize() < 1
                ? 10L : Math.min(query.getPageSize(), 50L);

        Page<AdminUserCreditVO> page = new Page<>(current, size);
        IPage<AdminUserCreditVO> result = userMapper.selectAdminUserPage(page,
                trim(query == null ? null : query.getKeyword()),
                upper(query == null ? null : query.getLevel()),
                upper(query == null ? null : query.getRiskType()));
        result.getRecords().forEach(vo -> vo.setRiskTypeLabel(riskLabel(vo.getRiskType())));
        return PageResult.of(result);
    }

    /** 风控概览：给后台页面顶部的小卡片用，全部实时统计 */
    public Map<String, Object> overview() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("overdueOrders", orderMapper.selectCount(Wrappers.<LeaseOrder>lambdaQuery()
                .eq(LeaseOrder::getStatus, "OVERDUE")));
        map.put("warningCount", countActive(TYPE_WARNING));
        map.put("blacklistCount", countActive(TYPE_BLACKLIST) + countActive(TYPE_OVERRIDE));
        map.put("pendingAppeals", appealMapper.selectCount(Wrappers.<CreditAppeal>lambdaQuery()
                .eq(CreditAppeal::getStatus, "PENDING")));
        return map;
    }

    private long countActive(String type) {
        return blacklistMapper.selectCount(Wrappers.<CreditBlacklist>lambdaQuery()
                .eq(CreditBlacklist::getListType, type)
                .eq(CreditBlacklist::getStatus, "ACTIVE"));
    }

    /* ==================== 内部方法 ==================== */

    private PageResult<CreditAppealVO> pageAppeals(AppealQueryDTO query, Long userId) {
        long current = query == null || query.getCurrent() == null || query.getCurrent() < 1
                ? 1L : query.getCurrent();
        long size = query == null || query.getPageSize() == null || query.getPageSize() < 1
                ? 10L : Math.min(query.getPageSize(), 50L);

        Page<CreditAppealVO> page = new Page<>(current, size);
        IPage<CreditAppealVO> result = appealMapper.selectAppealPage(page, userId,
                upper(query == null ? null : query.getStatus()),
                trim(query == null ? null : query.getKeyword()));
        result.getRecords().forEach(this::fillAppealLabels);
        return PageResult.of(result);
    }

    private CreditAppealVO fillAppealLabels(CreditAppealVO vo) {
        if (vo == null) {
            return null;
        }
        vo.setStatusLabel(appealStatusLabel(vo.getStatus()));
        return vo;
    }

    private void fillBlacklistLabels(CreditBlacklistVO vo) {
        vo.setListTypeLabel(typeLabel(vo.getListType()));
        vo.setRestrictOrder(TYPE_OVERRIDE.equals(vo.getListType()) || TYPE_BLACKLIST.equals(vo.getListType()));
        vo.setStatusLabel("ACTIVE".equals(vo.getStatus()) ? "生效中" : "已解除");
    }

    private String appealStatusLabel(String status) {
        if (status == null) {
            return "未知";
        }
        return switch (status) {
            case "PENDING" -> "待处理";
            case "APPROVED" -> "已通过";
            case "REJECTED" -> "已驳回";
            default -> status;
        };
    }

    private String typeLabel(String type) {
        if (type == null) {
            return "无";
        }
        return switch (type) {
            case TYPE_WARNING -> "预警";
            case TYPE_OVERRIDE -> "限制下单";
            case TYPE_BLACKLIST -> "黑名单";
            default -> type;
        };
    }

    private String riskLabel(String riskType) {
        return riskType == null ? "正常" : typeLabel(riskType);
    }

    private int calcOverdueDays(LocalDate endDate, LocalDate today) {
        if (endDate == null || today == null || !today.isAfter(endDate)) {
            return 0;
        }
        return (int) ChronoUnit.DAYS.between(endDate, today);
    }

    private String generateAppealNo() {
        String suffix = String.format("%04d", ThreadLocalRandom.current().nextInt(10_000));
        return "A" + LocalDateTime.now().format(NO_FMT) + suffix;
    }

    private String upper(String value) {
        return StringUtils.hasText(value) ? value.trim().toUpperCase() : null;
    }

    private String trim(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
