package com.zuche.module.order.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zuche.common.BusinessException;
import com.zuche.common.PageResult;
import com.zuche.common.ResultCode;
import com.zuche.common.enums.OrderStatus;
import com.zuche.common.enums.PayType;
import com.zuche.module.credit.dto.CreditEvent;
import com.zuche.module.credit.service.CreditService;
import com.zuche.module.credit.vo.CreditChangeVO;
import com.zuche.module.credit.vo.CreditSnapshot;
import com.zuche.module.order.dto.OrderCreateDTO;
import com.zuche.module.order.dto.OrderPayDTO;
import com.zuche.module.order.dto.OrderQueryDTO;
import com.zuche.module.order.dto.OrderQuoteDTO;
import com.zuche.module.order.entity.LeaseOrder;
import com.zuche.module.order.entity.OrderItem;
import com.zuche.module.order.mapper.LeaseOrderMapper;
import com.zuche.module.order.mapper.OrderItemMapper;
import com.zuche.module.order.vo.OrderCreateVO;
import com.zuche.module.order.vo.OrderCancelVO;
import com.zuche.module.order.vo.OrderDetailVO;
import com.zuche.module.order.vo.OrderListVO;
import com.zuche.module.order.vo.OrderPayVO;
import com.zuche.module.order.vo.OrderQuoteVO;
import com.zuche.module.order.vo.OrderReturnVO;
import com.zuche.module.order.vo.PaymentVO;
import com.zuche.module.payment.entity.PaymentRecord;
import com.zuche.module.payment.service.PaymentService;
import com.zuche.module.refund.service.RefundService;
import com.zuche.module.risk.service.RiskControlService;
import com.zuche.module.store.dto.StoreOrderQueryDTO;
import com.zuche.module.store.entity.Store;
import com.zuche.module.store.mapper.StoreMapper;
import com.zuche.module.store.vo.StoreOrderVO;
import com.zuche.module.vehicle.mapper.VehicleMapper;
import com.zuche.module.vehicle.vo.VehicleRentVO;
import com.zuche.security.LoginUser;
import com.zuche.security.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 租赁订单服务（S2：下单 → 模拟支付 → 车辆锁定）
 *
 * <p>三条贯穿全流程的硬规则：</p>
 * <ol>
 *   <li><b>金额只信后端</b>：前端只传车辆/日期/数量，单价、折扣、押金系数全部现查数据库。</li>
 *   <li><b>状态只能顺着走</b>：任何状态变更都先过 {@link OrderStateMachine}，
 *       杜绝"待支付直接变已完成"这种脏数据。</li>
 *   <li><b>车辆锁定用 CAS</b>：{@code UPDATE ... WHERE status = 1} 的影响行数决定成败，
 *       两个人同时付同一台车，只有一个人能成功。</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    /** 单笔订单最长租期，超过要走线下长租流程 */
    private static final int MAX_RENT_DAYS = 30;

    private static final DateTimeFormatter ORDER_NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private static final BigDecimal ZERO = new BigDecimal("0.00");

    private final LeaseOrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final VehicleMapper vehicleMapper;
    private final StoreMapper storeMapper;
    private final CreditService creditService;
    private final PaymentService paymentService;
    private final RefundService refundService;
    private final RiskControlService riskControlService;
    private final OrderStateMachine stateMachine;

    /* ==================== 1. 试算 ==================== */

    /**
     * 租赁试算：不落库，仅按当前用户信用等级实时算钱
     *
     * <p>押金 = 标准押金 × 信用系数 × 数量。系数来自 credit_rule，
     * 优秀 0.80 / 良好 0.90 / 一般 1.00 / 待观察 1.20，后台可改。</p>
     */
    public OrderQuoteVO quote(OrderQuoteDTO dto) {
        Long userId = UserContext.requireUserId();
        CreditSnapshot credit = creditService.getSnapshot(userId);
        return buildQuote(dto, credit);
    }

    /* ==================== 2. 下单 ==================== */

    /**
     * 创建订单：状态 WAIT_PAY，此时<b>不锁车</b>
     *
     * <p>不锁车是有意为之：未支付就占库存会出现"下单不付款把车锁死"的问题，
     * 而本项目没有支付超时释放的定时任务。真正的占用发生在支付成功那一刻的 CAS 锁车。</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public OrderCreateVO create(OrderCreateDTO dto) {
        Long userId = UserContext.requireUserId();
        // 风控拦截放在最前面：被拉黑的用户连试算都不该继续，更不该生成订单
        riskControlService.assertCanOrder(userId);
        CreditSnapshot credit = creditService.getSnapshot(userId);
        OrderQuoteVO q = buildQuote(dto, credit);

        Long returnStoreId = resolveReturnStore(dto.getReturnStoreId(), q.getStoreId());
        LocalDateTime now = LocalDateTime.now();

        LeaseOrder order = new LeaseOrder();
        order.setOrderNo(generateOrderNo());
        order.setUserId(userId);
        order.setStoreId(q.getStoreId());
        order.setReturnStoreId(returnStoreId);
        order.setStartDate(q.getStartDate());
        order.setEndDate(q.getEndDate());
        order.setRentDays(q.getRentDays());
        order.setVehicleCount(1);
        order.setRentAmount(q.getRentAmount());
        order.setDepositAmount(q.getDepositAmount());
        order.setTotalAmount(q.getTotalAmount());
        order.setCreditScoreSnapshot(credit.getCreditScore());
        order.setCreditLevelSnapshot(credit.getCreditLevel());
        order.setDepositCoefficient(q.getDepositCoefficient());
        order.setStatus(OrderStatus.WAIT_PAY.name());
        order.setOverdueDays(0);
        order.setLateFee(ZERO);
        order.setDepositRefundStatus("NONE");
        order.setRemark(dto.getRemark());
        order.setCreateTime(now);
        order.setUpdateTime(now);
        orderMapper.insert(order);

        OrderItem item = new OrderItem();
        item.setOrderId(order.getId());
        item.setVehicleId(q.getVehicleId());
        item.setVehicleName(q.getVehicleName());
        item.setLicensePlate(q.getLicensePlate());
        item.setDailyPrice(q.getDailyPrice());
        item.setQuantity(q.getQuantity());
        item.setRentDays(q.getRentDays());
        item.setDiscountRate(q.getDiscountRate());
        item.setRentAmount(q.getRentAmount());
        item.setDepositAmount(q.getDepositAmount());
        item.setCreateTime(now);
        item.setUpdateTime(now);
        orderItemMapper.insert(item);

        log.info("创建订单: orderNo={}, userId={}, vehicleId={}, 租金={}, 押金={}(系数{}), 信用等级={}",
                order.getOrderNo(), userId, q.getVehicleId(), q.getRentAmount(), q.getDepositAmount(),
                q.getDepositCoefficient(), credit.getCreditLevel());

        OrderCreateVO vo = new OrderCreateVO();
        vo.setOrderId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setStatus(order.getStatus());
        vo.setStatusLabel(OrderStatus.WAIT_PAY.getLabel());
        vo.setRentAmount(order.getRentAmount());
        vo.setDepositAmount(order.getDepositAmount());
        vo.setTotalAmount(order.getTotalAmount());
        vo.setCreateTime(now);
        return vo;
    }

    /* ==================== 3. 模拟支付 ==================== */

    /**
     * 模拟支付：WAIT_PAY → WAIT_PICKUP
     *
     * <p>事务内四步，缺一不可：</p>
     * <ol>
     *   <li>行锁读订单（防连点重复支付）</li>
     *   <li>状态机断言 + 已支付流水检查</li>
     *   <li>CAS 锁车：任一车辆抢不到就整单回滚</li>
     *   <li>写 RENT + DEPOSIT 两条流水并推进状态</li>
     * </ol>
     */
    @Transactional(rollbackFor = Exception.class)
    public OrderPayVO pay(Long orderId, OrderPayDTO dto) {
        Long userId = UserContext.requireUserId();

        // 1) 行锁：同一订单的并发请求在这里串行化
        LeaseOrder order = orderMapper.selectByIdForUpdate(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "订单不存在");
        }

        // 2) 只有"待支付"能流转到"待取车"，其余状态一律拒绝
        OrderStatus current = OrderStatus.of(order.getStatus());
        stateMachine.assertTransfer(current, OrderStatus.WAIT_PICKUP);
        paymentService.assertNotPaid(orderId);

        // 3) 锁车：CAS 失败说明车已被别人租走，必须整单回滚
        List<OrderItem> items = orderItemMapper.selectList(Wrappers.<OrderItem>lambdaQuery()
                .eq(OrderItem::getOrderId, orderId));
        if (items.isEmpty()) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "订单明细缺失，无法支付");
        }
        for (OrderItem item : items) {
            if (vehicleMapper.lockForRent(item.getVehicleId()) == 0) {
                throw new BusinessException(ResultCode.BUSINESS_ERROR,
                        "车辆「" + item.getVehicleName() + "」已被租出或不可租，请取消订单后重新选择");
            }
        }

        // 4) 生成租金与押金两条流水
        LocalDateTime payTime = LocalDateTime.now();
        List<PaymentRecord> records = paymentService.mockPay(order, dto == null ? null : dto.getPayMethod());

        LeaseOrder update = new LeaseOrder();
        update.setId(orderId);
        update.setStatus(OrderStatus.WAIT_PICKUP.name());
        update.setPayTime(payTime);
        update.setUpdateTime(payTime);
        orderMapper.updateById(update);

        // ==================== S3 信用事件接入点 ====================
        // 支付本身不产生信用事件（信用分只在 完成/按时归还/逾期/已支付取消 时变化），
        // 所以这里不调用 recordEvent。S3 在上述流程里按下面的方式接入即可，
        // 注意 dedupKey 必须唯一，否则定时任务重跑会重复加减分：
        //
        //   CreditEvent event = new CreditEvent();
        //   event.setUserId(userId);
        //   event.setOrderId(orderId);
        //   event.setActionCode("ORDER_FINISH");          // 见 credit_rule.rule_code
        //   event.setDedupKey("finish-" + orderId);       // 与 order_id + action_code 组成唯一索引
        //   event.setRemark("订单完成，自动加分");
        //   creditService.recordEvent(event);             // S2 为空实现：只打日志，不改分
        // ==========================================================

        OrderPayVO vo = new OrderPayVO();
        vo.setOrderId(orderId);
        vo.setOrderNo(order.getOrderNo());
        vo.setStatus(OrderStatus.WAIT_PICKUP.name());
        vo.setStatusLabel(OrderStatus.WAIT_PICKUP.getLabel());
        vo.setPayTime(payTime);
        vo.setPaidAmount(order.getTotalAmount());
        vo.setPayments(records.stream().map(this::toPaymentVO).toList());
        return vo;
    }

    /* ==================== 4. 取消 ==================== */

    /**
     * 取消订单
     *
     * <p>两种情况走同一个入口，但处理完全不同：</p>
     * <ul>
     *   <li><b>待支付</b>：没付过钱，只释放车辆，不产生退款、不动信用分</li>
     *   <li><b>待取车（已支付）</b>：钱已经收了、车已经锁了，必须
     *       ① 租金和押金各退一笔 ② 释放车辆 ③ 扣 CANCEL_PAID 的分
     *       ④ 重算行为分（取消率变了）</li>
     * </ul>
     *
     * <p>已取车（IN_USE）之后不允许取消：车已经在用户手上，
     * 只能通过归还流程闭环，否则会出现"车没还、钱退了"的资产黑洞。</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public OrderCancelVO cancel(Long orderId, String reason) {
        Long userId = UserContext.requireUserId();
        LeaseOrder order = orderMapper.selectByIdForUpdate(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "订单不存在");
        }

        OrderStatus current = OrderStatus.of(order.getStatus());
        String cancelReason = StringUtils.hasText(reason) ? reason : "用户主动取消";

        if (current == OrderStatus.WAIT_PAY) {
            // 未支付：只释放车辆，不涉及资金与信用
            stateMachine.assertTransfer(current, OrderStatus.CANCELLED);
            releaseVehicles(orderId);
            updateCancel(orderId, cancelReason, "NONE");

            log.info("取消未支付订单: orderNo={}, reason={}", order.getOrderNo(), cancelReason);
            return buildCancelVO(order, ZERO, null, null);
        }

        if (current == OrderStatus.WAIT_PICKUP) {
            // 已支付：先退款、再释放车辆、最后动信用分（顺序不能反，保证有钱退才扣分）
            stateMachine.assertTransfer(current, OrderStatus.CANCELLED);
            BigDecimal refunded = refundPaidOrder(order, cancelReason);
            releaseVehicles(orderId);
            updateCancel(orderId, cancelReason, "REFUNDED");

            CreditChangeVO eventChange = creditService.recordEvent(buildEvent(
                    order, "CANCEL_PAID", "cancel-paid", "支付后取消订单，占用车辆库存"));
            CreditChangeVO behaviorChange = creditService.refitBehavior(userId, orderId);

            log.info("取消已支付订单: orderNo={}, 退款={}, 信用 {}→{}",
                    order.getOrderNo(), refunded,
                    eventChange == null ? null : eventChange.getScoreBefore(),
                    eventChange == null ? null : eventChange.getScoreAfter());
            return buildCancelVO(order, refunded, eventChange, behaviorChange);
        }

        throw new BusinessException(ResultCode.BUSINESS_ERROR,
                "订单当前为「" + current.getLabel() + "」，不支持取消；已取车的订单请走归还流程");
    }

    /* ==================== 4.1 取车 / 归还 ==================== */

    /**
     * 确认取车：WAIT_PICKUP → IN_USE
     *
     * <p>真实业务里这一步由网点员工操作（扫车牌/验证件），本项目按自助取车实现，
     * 车辆状态维持"已租出"，还车时才释放。</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public void pickup(Long orderId) {
        doPickup(requireOwnOrderForUpdate(orderId));
    }

    /**
     * 网点办理交付：管理员只能操作本网点订单
     *
     * <p>与 {@link #pickup(Long)} 共用 {@link #doPickup}，差别只在归属校验：
     * 用户端校验 {@code user_id}，网点端校验 {@code store_id}。
     * 状态机断言、时间戳落库完全一致，不会出现"两个入口两套规则"。</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public void pickupByStore(Long orderId) {
        doPickup(requireStoreOrderForUpdate(orderId));
    }

    private void doPickup(LeaseOrder order) {
        Long orderId = order.getId();
        OrderStatus current = OrderStatus.of(order.getStatus());
        stateMachine.assertTransfer(current, OrderStatus.IN_USE);

        LocalDateTime now = LocalDateTime.now();
        LeaseOrder update = new LeaseOrder();
        update.setId(orderId);
        update.setStatus(OrderStatus.IN_USE.name());
        update.setPickupTime(now);
        update.setUpdateTime(now);
        orderMapper.updateById(update);

        log.info("确认取车: orderNo={}", order.getOrderNo());
    }

    /**
     * 申请归还：IN_USE / OVERDUE → WAIT_RETURN
     *
     * <p>拆成"申请"和"确认"两步，是为了让逾期判定有明确的时间锚点：
     * 申请时刻记录在 return_apply_time，结算时用实际归还日期与计划还车日期比较。</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public void applyReturn(Long orderId) {
        LeaseOrder order = requireOwnOrderForUpdate(orderId);
        OrderStatus current = OrderStatus.of(order.getStatus());
        stateMachine.assertTransfer(current, OrderStatus.WAIT_RETURN);

        LocalDateTime now = LocalDateTime.now();
        LeaseOrder update = new LeaseOrder();
        update.setId(orderId);
        update.setStatus(OrderStatus.WAIT_RETURN.name());
        update.setReturnApplyTime(now);
        update.setUpdateTime(now);
        orderMapper.updateById(update);

        log.info("申请归还: orderNo={}", order.getOrderNo());
    }

    /**
     * 确认归还并结算：WAIT_RETURN → RETURNED → COMPLETED
     *
     * <p>这一个事务里要完成六件事，任何一步失败都整体回滚：</p>
     * <ol>
     *   <li>算逾期天数（实际归还日 - 计划还车日）</li>
     *   <li>按天的违约金从押金中扣除，得到实退金额（封顶为押金，不会出现负退款）</li>
     *   <li>状态推进两跳，RETURNED 落库后再进 COMPLETED，保证流转可追溯</li>
     *   <li>释放车辆</li>
     *   <li>退还押金并写退款流水，退款原因里写明扣了多少违约金</li>
     *   <li>信用结算：逾期按天扣（受 max_times 限制）→ 归还事件 → 行为分重算</li>
     * </ol>
     */
    @Transactional(rollbackFor = Exception.class)
    public OrderReturnVO confirmReturn(Long orderId) {
        return doConfirmReturn(requireOwnOrderForUpdate(orderId));
    }

    /**
     * 网点确认归还并结算：只能操作本网点订单
     *
     * <p>结算逻辑与用户端共用 {@link #doConfirmReturn}——违约金、押金退款、
     * 信用结算必须只有一套实现，否则两个入口算出来的钱会对不上。</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public OrderReturnVO confirmReturnByStore(Long orderId) {
        return doConfirmReturn(requireStoreOrderForUpdate(orderId));
    }

    private OrderReturnVO doConfirmReturn(LeaseOrder order) {
        Long orderId = order.getId();
        OrderStatus current = OrderStatus.of(order.getStatus());
        // 逾期订单可以直接确认归还：车必须还得回来，不能因为状态是 OVERDUE 就卡住流程
        if (current != OrderStatus.WAIT_RETURN && current != OrderStatus.OVERDUE) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR,
                    "仅「待归还」或「逾期」的订单可以确认归还，当前状态为「" + current.getLabel() + "」");
        }
        // 两跳都要校验：WAIT_RETURN/OVERDUE → RETURNED → COMPLETED
        stateMachine.assertTransfer(current, OrderStatus.RETURNED);
        stateMachine.assertTransfer(OrderStatus.RETURNED, OrderStatus.COMPLETED);

        LocalDateTime now = LocalDateTime.now();
        int overdueDays = calcOverdueDays(order.getEndDate(), now.toLocalDate());

        // 押金与违约金要在落库前算好：违约金从押金里扣，扣完为止，不能出现负数退款
        PaymentRecord depositPayment = paymentService.findSuccessPayment(orderId, PayType.DEPOSIT);
        BigDecimal depositAmount = depositPayment == null ? ZERO : depositPayment.getAmount();
        BigDecimal lateFee = calcLateFee(depositAmount, overdueDays);
        BigDecimal depositRefunded = money(depositAmount.subtract(lateFee).max(ZERO));

        // 第一跳：先落 RETURNED，保留归还时间这个事实
        LeaseOrder returned = new LeaseOrder();
        returned.setId(orderId);
        returned.setStatus(OrderStatus.RETURNED.name());
        returned.setReturnTime(now);
        returned.setOverdueDays(overdueDays);
        returned.setLateFee(lateFee);
        returned.setUpdateTime(now);
        orderMapper.updateById(returned);

        // 释放车辆（只释放自己锁的那台，CAS 条件 status = 2）
        releaseVehicles(orderId);

        // 退还押金：实退 = 押金 - 违约金
        refundService.refund(order, "DEPOSIT", depositRefunded,
                depositPayment == null ? null : depositPayment.getId(),
                buildDepositRefundReason(overdueDays, lateFee, depositRefunded));

        // 第二跳：结算完成
        LeaseOrder completed = new LeaseOrder();
        completed.setId(orderId);
        completed.setStatus(OrderStatus.COMPLETED.name());
        completed.setFinishTime(now);
        completed.setDepositRefundStatus("REFUNDED");
        completed.setUpdateTime(now);
        orderMapper.updateById(completed);

        List<CreditChangeVO> changes = settleCredit(order, overdueDays);

        log.info("归还结算完成: orderNo={}, 逾期={}天, 押金={}, 违约金={}, 实退={}, 信用变动={}条",
                order.getOrderNo(), overdueDays, depositAmount, lateFee, depositRefunded, changes.size());

        OrderReturnVO vo = new OrderReturnVO();
        vo.setOrderId(orderId);
        vo.setOrderNo(order.getOrderNo());
        vo.setStatus(OrderStatus.COMPLETED.name());
        vo.setStatusLabel(OrderStatus.COMPLETED.getLabel());
        vo.setOverdueDays(overdueDays);
        vo.setDepositAmount(depositAmount);
        vo.setLateFee(lateFee);
        vo.setDepositRefunded(depositRefunded);
        vo.setCreditChanges(changes);
        return vo;
    }

    /* ==================== 5. 查询 ==================== */

    public PageResult<OrderListVO> myOrders(OrderQueryDTO query) {
        Long userId = UserContext.requireUserId();
        Page<OrderListVO> page = new Page<>(query.getCurrent(), query.getPageSize());
        IPage<OrderListVO> result = orderMapper.selectOrderPage(page, userId, normalizeStatus(query.getStatus()));

        Map<String, String> levelLabels = creditService.levelLabelMap();
        result.getRecords().forEach(vo -> fillLabels(vo, levelLabels));
        return PageResult.of(result);
    }

    /**
     * 网点订单分页：只返回本网点的单
     *
     * <p>复用 {@link #fillLabels} 计算按钮标志，但网点视角下含义不同：
     * {@code canPickup} 表示"可办理交付"，{@code canConfirmReturn} 表示"可办理归还"。
     * 用户端的"支付/取消"在这里没有意义，前端不渲染即可。</p>
     */
    public PageResult<StoreOrderVO> storeOrders(StoreOrderQueryDTO query) {
        Long storeId = requireStoreId();
        Page<StoreOrderVO> page = new Page<>(query.getCurrent(), query.getPageSize());
        IPage<StoreOrderVO> result = orderMapper.selectStoreOrderPage(
                page, storeId, normalizeStatus(query.getStatus()), trimToNull(query.getKeyword()));

        Map<String, String> levelLabels = creditService.levelLabelMap();
        result.getRecords().forEach(vo -> fillLabels(vo, levelLabels));
        return PageResult.of(result);
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public OrderDetailVO detail(Long orderId) {
        Long userId = UserContext.requireUserId();
        OrderDetailVO vo = orderMapper.selectOrderDetail(orderId, userId);
        if (vo == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "订单不存在");
        }
        fillLabels(vo, creditService.levelLabelMap());
        vo.setItems(orderItemMapper.selectItemVO(orderId));
        vo.setPayments(paymentService.listByOrder(orderId));
        vo.setRefunds(refundService.listByOrder(orderId));

        // 违约金只在归还结算那一刻产生。未结算的订单给一个按当前规则算出的"预估"，
        // 避免把库里可能存在的历史值或 0 当成实际扣款展示（预估也会随天数增长）
        OrderStatus detailStatus = OrderStatus.of(vo.getStatus());
        boolean lateFeeSettled = detailStatus == OrderStatus.COMPLETED || detailStatus == OrderStatus.RETURNED;
        vo.setLateFeeSettled(lateFeeSettled);
        if (!lateFeeSettled) {
            int overdueDays = calcOverdueDays(vo.getEndDate(), LocalDate.now());
            vo.setOverdueDays(overdueDays);
            vo.setLateFee(calcLateFee(vo.getDepositAmount() == null ? ZERO : vo.getDepositAmount(), overdueDays));
        }

        // 押金标准合计 = 应付押金 / 系数，反推出"如果没有信用折扣要交多少"
        BigDecimal coefficient = vo.getDepositCoefficient();
        if (coefficient != null && coefficient.compareTo(BigDecimal.ZERO) > 0
                && vo.getDepositAmount() != null) {
            BigDecimal standard = vo.getDepositAmount().divide(coefficient, 4, RoundingMode.HALF_UP)
                    .setScale(2, RoundingMode.HALF_UP);
            vo.setDepositStandardTotal(standard);
            vo.setDepositSaved(money(standard.subtract(vo.getDepositAmount())));
        }
        return vo;
    }

    /* ==================== 内部方法 ==================== */

    /** 行锁 + 归属校验：所有状态变更入口的第一步，防止越权操作别人的订单 */
    private LeaseOrder requireOwnOrderForUpdate(Long orderId) {
        Long userId = UserContext.requireUserId();
        LeaseOrder order = orderMapper.selectByIdForUpdate(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "订单不存在");
        }
        return order;
    }

    /**
     * 行锁 + 网点归属校验：网点管理员只能操作本网点订单
     *
     * <p>返回 404 而不是 403：告诉调用方"这个订单不存在于你的管辖范围"就足够了，
     * 不暴露"订单确实存在、只是不归你"这种信息，避免被用来探测他人订单号。</p>
     */
    private LeaseOrder requireStoreOrderForUpdate(Long orderId) {
        Long storeId = requireStoreId();
        LeaseOrder order = orderMapper.selectByIdForUpdate(orderId);
        if (order == null || !storeId.equals(order.getStoreId())) {
            throw new BusinessException(ResultCode.NOT_FOUND, "本网点不存在该订单");
        }
        return order;
    }

    /** 取当前登录网点管理员的网点 ID；非网点管理员直接 403 */
    private Long requireStoreId() {
        LoginUser login = UserContext.get();
        if (login == null || !login.isStoreAdmin() || login.getStoreId() == null) {
            throw new BusinessException(ResultCode.FORBIDDEN, "仅网点管理员可访问该功能");
        }
        return login.getStoreId();
    }

    /** 释放订单占用的车辆（CAS：只把 status=2 的改回 1，不误放维修中的车） */
    private void releaseVehicles(Long orderId) {
        List<OrderItem> items = orderItemMapper.selectList(Wrappers.<OrderItem>lambdaQuery()
                .eq(OrderItem::getOrderId, orderId));
        for (OrderItem item : items) {
            vehicleMapper.releaseRent(item.getVehicleId());
        }
    }

    private void updateCancel(Long orderId, String reason, String depositRefundStatus) {
        LocalDateTime now = LocalDateTime.now();
        LeaseOrder update = new LeaseOrder();
        update.setId(orderId);
        update.setStatus(OrderStatus.CANCELLED.name());
        update.setCancelTime(now);
        update.setCancelReason(reason);
        update.setDepositRefundStatus(depositRefundStatus);
        update.setUpdateTime(now);
        orderMapper.updateById(update);
    }

    /**
     * 已支付订单取消时的退款：租金 + 押金各一笔，分别回指原支付流水
     *
     * <p>不合并成一笔，是为了让"押金池"和"营收"两个口径始终能对上：
     * 租金退的是收入冲回，押金退的是负债减少，财务上不是一回事。</p>
     */
    private BigDecimal refundPaidOrder(LeaseOrder order, String reason) {
        BigDecimal total = ZERO;
        PaymentRecord rent = paymentService.findSuccessPayment(order.getId(), PayType.RENT);
        if (rent != null) {
            refundService.refund(order, "CANCEL", rent.getAmount(), rent.getId(),
                    "订单取消，退还租金：" + reason);
            total = total.add(rent.getAmount());
        }
        PaymentRecord deposit = paymentService.findSuccessPayment(order.getId(), PayType.DEPOSIT);
        if (deposit != null) {
            refundService.refund(order, "DEPOSIT", deposit.getAmount(), deposit.getId(),
                    "订单取消，退还押金");
            total = total.add(deposit.getAmount());
        }
        return money(total);
    }

    /**
     * 归还时的信用结算
     *
     * <p>规则全部来自 credit_rule，这里只负责"什么情况下发什么事件"：</p>
     * <ul>
     *   <li>逾期：每天一条 OVERDUE 事件，条数受 max_times 限制（默认 7 天封顶）</li>
     *   <li>逾期归还：OVERDUE_RETURN（负分）</li>
     *   <li>按时归还：ON_TIME_RETURN（正分）+ ORDER_FINISH（正分）</li>
     * </ul>
     *
     * <p>逾期订单不给 ORDER_FINISH：流程虽然走完了，但履约质量不合格，
     * 正向事件只奖励按时履约的用户。</p>
     */
    private List<CreditChangeVO> settleCredit(LeaseOrder order, int overdueDays) {
        List<CreditChangeVO> changes = new ArrayList<>();

        if (overdueDays > 0) {
            int maxTimes = creditService.maxTimes("OVERDUE");
            int times = Math.min(overdueDays, maxTimes);
            for (int day = 1; day <= times; day++) {
                addChange(changes, creditService.recordEvent(buildEvent(order, "OVERDUE",
                        "overdue-day-" + day, "逾期未归还第 " + day + " 天")));
            }
            addChange(changes, creditService.recordEvent(buildEvent(order, "OVERDUE_RETURN",
                    "overdue-return", "逾期 " + overdueDays + " 天后归还")));
        } else {
            addChange(changes, creditService.recordEvent(buildEvent(order, "ON_TIME_RETURN",
                    "on-time-return", "按时归还")));
            addChange(changes, creditService.recordEvent(buildEvent(order, "ORDER_FINISH",
                    "order-finish", "订单完成，取车-归还-结算全流程走完")));
        }

        // 行为特征（按时率/取消率/逾期次数）变了，重算行为分
        addChange(changes, creditService.refitBehavior(order.getUserId(), order.getId()));
        return changes;
    }

    private void addChange(List<CreditChangeVO> changes, CreditChangeVO change) {
        if (change != null && !Boolean.TRUE.equals(change.getDuplicated())) {
            changes.add(change);
        }
    }

    private CreditEvent buildEvent(LeaseOrder order, String actionCode, String dedupKey, String remark) {
        CreditEvent event = new CreditEvent();
        event.setUserId(order.getUserId());
        event.setOrderId(order.getId());
        event.setActionCode(actionCode);
        event.setDedupKey(dedupKey);
        event.setRemark(remark);
        return event;
    }

    private OrderCancelVO buildCancelVO(LeaseOrder order, BigDecimal refundAmount,
                                        CreditChangeVO eventChange, CreditChangeVO behaviorChange) {
        OrderCancelVO vo = new OrderCancelVO();
        vo.setOrderId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setStatus(OrderStatus.CANCELLED.name());
        vo.setStatusLabel(OrderStatus.CANCELLED.getLabel());
        vo.setRefundAmount(refundAmount);
        vo.setCreditChange(eventChange);
        vo.setBehaviorChange(behaviorChange);
        return vo;
    }

    /** 逾期天数 = 实际归还日 - 计划还车日，提前归还记 0 */
    private int calcOverdueDays(LocalDate endDate, LocalDate returnDate) {
        if (endDate == null || returnDate == null || !returnDate.isAfter(endDate)) {
            return 0;
        }
        return (int) ChronoUnit.DAYS.between(endDate, returnDate);
    }

    /**
     * 逾期违约金 = 单价（元/天）× 逾期天数，封顶为押金总额
     *
     * <p>单价与最多计费天数都来自 credit_rule 的 LATE_FEE_DAILY 参数：
     * 后台把 200 改成 300，下一笔归还立刻按新标准执行，Java 里没有常量。
     * 押金扣完为止——不能出现"用户倒欠平台钱"的负退款。</p>
     */
    private BigDecimal calcLateFee(BigDecimal deposit, int overdueDays) {
        if (overdueDays <= 0 || deposit == null || deposit.signum() <= 0) {
            return ZERO;
        }
        int daily = creditService.param("LATE_FEE_DAILY", 0);
        if (daily <= 0) {
            return ZERO;
        }
        int maxDays = creditService.maxTimes("LATE_FEE_DAILY");
        int days = Math.min(overdueDays, maxDays);
        BigDecimal fee = BigDecimal.valueOf(daily).multiply(BigDecimal.valueOf(days));
        return money(fee.min(deposit));
    }

    private String buildDepositRefundReason(int overdueDays, BigDecimal lateFee, BigDecimal refunded) {
        if (overdueDays <= 0) {
            return "订单完成，押金全额退还";
        }
        if (lateFee.signum() <= 0) {
            return "订单完成，押金全额退还（逾期 " + overdueDays + " 天，未启用违约金）";
        }
        return String.format("订单完成，押金扣除逾期 %d 天违约金 %s 元后，实退 %s 元",
                overdueDays, lateFee.toPlainString(), refunded.toPlainString());
    }

    private OrderQuoteVO buildQuote(OrderQuoteDTO dto, CreditSnapshot credit) {
        if (dto.getStartDate() == null || dto.getEndDate() == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "请选择完整的租期");
        }
        VehicleRentVO vehicle = requireRentVehicle(dto.getVehicleId());
        int rentDays = calcRentDays(dto.getStartDate(), dto.getEndDate());
        int quantity = normalizeQuantity(dto.getQuantity());

        BigDecimal discount = vehicle.getDiscountRate() == null ? BigDecimal.ONE : vehicle.getDiscountRate();
        BigDecimal coefficient = credit.getDepositCoefficient() == null
                ? BigDecimal.ONE : credit.getDepositCoefficient();
        BigDecimal dailyPrice = vehicle.getDailyPrice() == null ? ZERO : vehicle.getDailyPrice();
        BigDecimal depositStandard = vehicle.getDeposit() == null ? ZERO : vehicle.getDeposit();

        BigDecimal finalDailyPrice = money(dailyPrice.multiply(discount));
        BigDecimal rentAmount = money(dailyPrice
                .multiply(BigDecimal.valueOf(rentDays))
                .multiply(BigDecimal.valueOf(quantity))
                .multiply(discount));
        BigDecimal depositStandardTotal = money(depositStandard.multiply(BigDecimal.valueOf(quantity)));
        BigDecimal depositAmount = money(depositStandard
                .multiply(coefficient)
                .multiply(BigDecimal.valueOf(quantity)));
        BigDecimal depositSaved = money(depositStandardTotal.subtract(depositAmount));

        OrderQuoteVO vo = new OrderQuoteVO();
        vo.setVehicleId(vehicle.getId());
        vo.setVehicleName(vehicle.getVehicleName());
        vo.setBrandName(vehicle.getBrandName());
        vo.setModelName(vehicle.getModelName());
        vo.setLicensePlate(vehicle.getLicensePlate());
        vo.setCoverImage(vehicle.getCoverImage());
        vo.setStoreId(vehicle.getStoreId());
        vo.setStoreName(vehicle.getStoreName());
        vo.setStartDate(dto.getStartDate());
        vo.setEndDate(dto.getEndDate());
        vo.setRentDays(rentDays);
        vo.setQuantity(quantity);
        vo.setDailyPrice(dailyPrice);
        vo.setDiscountRate(discount);
        vo.setFinalDailyPrice(finalDailyPrice);
        vo.setRentAmount(rentAmount);
        vo.setDepositStandard(depositStandard);
        vo.setDepositCoefficient(coefficient);
        vo.setDepositAmount(depositAmount);
        vo.setDepositSaved(depositSaved);
        vo.setTotalAmount(money(rentAmount.add(depositAmount)));
        vo.setCredit(credit);
        return vo;
    }

    private VehicleRentVO requireRentVehicle(Long vehicleId) {
        if (vehicleId == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "请选择车辆");
        }
        VehicleRentVO vehicle = vehicleMapper.selectRentVehicle(vehicleId);
        if (vehicle == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "车辆不存在或已下架");
        }
        if (vehicle.getStatus() == null || vehicle.getStatus() != 1) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "该车辆当前不可租（已租出或维修中），请选择其他车辆");
        }
        return vehicle;
    }

    private int calcRentDays(LocalDate startDate, LocalDate endDate) {
        if (startDate.isBefore(LocalDate.now())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "取车日期不能早于今天");
        }
        long days = ChronoUnit.DAYS.between(startDate, endDate);
        if (days <= 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "还车日期必须晚于取车日期");
        }
        if (days > MAX_RENT_DAYS) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "单笔订单最长租期 " + MAX_RENT_DAYS + " 天");
        }
        return (int) days;
    }

    /**
     * S2 下单入口以"具体车辆"为单位（vehicle 表一行就是一台带唯一车牌的车），
     * quantity 只允许 1；字段保留，供后续"按车型批量下单"扩展。
     */
    private int normalizeQuantity(Integer quantity) {
        if (quantity == null || quantity < 1) {
            return 1;
        }
        if (quantity != 1) {
            throw new BusinessException(ResultCode.BAD_REQUEST,
                    "当前为单车租赁，数量必须为 1 台");
        }
        return 1;
    }

    private Long resolveReturnStore(Long returnStoreId, Long defaultStoreId) {
        if (returnStoreId == null) {
            return defaultStoreId;
        }
        Store store = storeMapper.selectById(returnStoreId);
        if (store == null || store.getStatus() == null || store.getStatus() != 1) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "归还网点不存在或已停业");
        }
        return returnStoreId;
    }

    private void fillLabels(OrderListVO vo, Map<String, String> levelLabels) {
        OrderStatus status = OrderStatus.of(vo.getStatus());
        vo.setStatusLabel(status.getLabel());
        // 各状态可执行的动作，前端据此决定按钮显隐（不做二次判断，避免按钮点了才报错）
        vo.setCanPay(status == OrderStatus.WAIT_PAY);
        vo.setCanCancel(status == OrderStatus.WAIT_PAY || status == OrderStatus.WAIT_PICKUP);
        vo.setCanPickup(status == OrderStatus.WAIT_PICKUP);
        vo.setCanApplyReturn(status == OrderStatus.IN_USE);
        vo.setCanConfirmReturn(status == OrderStatus.WAIT_RETURN || status == OrderStatus.OVERDUE);
        String level = vo.getCreditLevelSnapshot();
        vo.setCreditLevelLabel(levelLabels.getOrDefault(level, level));
    }

    private PaymentVO toPaymentVO(PaymentRecord record) {
        PaymentVO vo = new PaymentVO();
        vo.setId(record.getId());
        vo.setPayNo(record.getPayNo());
        vo.setPayType(record.getPayType());
        vo.setPayTypeLabel(paymentService.payTypeLabel(record.getPayType()));
        vo.setAmount(record.getAmount());
        vo.setPayMethod(record.getPayMethod());
        vo.setStatus(record.getStatus());
        vo.setPayTime(record.getPayTime());
        vo.setRemark(record.getRemark());
        return vo;
    }

    private String normalizeStatus(String status) {
        if (!StringUtils.hasText(status) || "ALL".equalsIgnoreCase(status)) {
            return null;
        }
        try {
            return OrderStatus.of(status.trim().toUpperCase()).name();
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "不支持的订单状态：" + status);
        }
    }

    /** 订单号：ZC + 精确到秒 + 6 位随机，配合 uk_order_no 兜底 */
    private String generateOrderNo() {
        String suffix = String.format("%06d", ThreadLocalRandom.current().nextInt(1_000_000));
        return "ZC" + LocalDateTime.now().format(ORDER_NO_FMT) + suffix;
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
