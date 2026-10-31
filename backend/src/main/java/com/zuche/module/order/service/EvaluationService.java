package com.zuche.module.order.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zuche.common.BusinessException;
import com.zuche.common.PageResult;
import com.zuche.common.ResultCode;
import com.zuche.common.enums.OrderStatus;
import com.zuche.module.order.dto.EvaluationCreateDTO;
import com.zuche.module.order.dto.EvaluationQueryDTO;
import com.zuche.module.order.entity.LeaseOrder;
import com.zuche.module.order.entity.OrderEvaluation;
import com.zuche.module.order.entity.OrderItem;
import com.zuche.module.order.mapper.LeaseOrderMapper;
import com.zuche.module.order.mapper.OrderEvaluationMapper;
import com.zuche.module.order.mapper.OrderItemMapper;
import com.zuche.module.order.vo.EvaluationStatsVO;
import com.zuche.module.order.vo.EvaluationVO;
import com.zuche.module.order.vo.VehicleEvaluationItemVO;
import com.zuche.module.order.vo.VehicleEvaluationVO;
import com.zuche.security.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 评价：用户写、所有人看、网点解释、管理员管违规
 *
 * <p>权限边界是这一块最重要的事：</p>
 * <ul>
 *   <li>用户：只能给<b>自己的、已完成的</b>订单写一条评价，一单一评；</li>
 *   <li>网点管理员：只能<b>回复</b>本店的评价，看不到别家，也不能删；</li>
 *   <li>系统管理员：能看全部、能回复、能删除违规内容（逻辑删除，留痕可追溯）。</li>
 * </ul>
 *
 * <p>删除权只给平台，是因为让商家删差评等于既当运动员又当裁判——
 * 差评一消失，用户不再信任评分，平台也就失去了改进依据。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EvaluationService {

    /** 星级文案：与前端评论文案保持一致，避免同一句话两处维护 */
    private static final List<String> RATING_LABELS =
            List.of("很不满意", "不满意", "一般", "满意", "非常满意");

    private static final ObjectMapper JSON = new ObjectMapper();

    private final OrderEvaluationMapper evaluationMapper;
    private final LeaseOrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;

    /* ==================== 用户端 ==================== */

    /**
     * 提交评价
     *
     * <p>三重校验缺一不可：订单是不是你的、订单做完了没有、是不是已经评过。
     * 前两条防止刷分，第三条靠 {@code uk_order} 兜底——两个标签页同时点提交，
     * 后到的那条会被数据库拦下，业务层只负责把它翻译成人话。</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public EvaluationVO create(Long orderId, EvaluationCreateDTO dto) {
        Long userId = UserContext.requireUserId();

        LeaseOrder order = orderMapper.selectById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "订单不存在");
        }
        if (!OrderStatus.COMPLETED.name().equals(order.getStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "订单完成后才能评价");
        }
        if (evaluationMapper.countByOrderId(orderId) > 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "这笔订单已经评价过了");
        }

        List<OrderItem> items = orderItemMapper.selectList(
                Wrappers.<OrderItem>lambdaQuery().eq(OrderItem::getOrderId, orderId));
        if (items.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "订单没有车辆明细，无法评价");
        }

        OrderEvaluation entity = new OrderEvaluation();
        entity.setOrderId(orderId);
        entity.setUserId(userId);
        // 评价挂在首台车上：车辆详情页按 vehicle_id 查评价，多车订单也只归到一台
        entity.setVehicleId(items.get(0).getVehicleId());
        entity.setVehicleRating(dto.getVehicleRating());
        entity.setStoreRating(dto.getStoreRating());
        entity.setRating(avgRating(dto.getVehicleRating(), dto.getStoreRating()));
        entity.setContent(dto.getContent().trim());
        entity.setImages(toJson(dto.getImages()));
        entity.setStatus(1);

        try {
            evaluationMapper.insert(entity);
        } catch (DuplicateKeyException dup) {
            // 唯一索引 uk_order 拦下的情况：并发提交，或该单旧评价被删后又被重新提交
            throw new BusinessException(ResultCode.BAD_REQUEST, "这笔订单已经评价过了");
        }

        log.info("提交评价: orderId={}, userId={}, 车={}分 店={}分",
                orderId, userId, dto.getVehicleRating(), dto.getStoreRating());
        return decorate(evaluationMapper.selectEvalById(entity.getId()));
    }

    /**
     * 车辆评价区块（前台公开）
     *
     * <p>只返回 status=1 的评价，评价人姓名在 SQL 里就脱敏成「张**」，
     * 手机号和订单号不出现在结果集里——前端"记得别渲染"不算安全措施。</p>
     */
    public VehicleEvaluationVO vehicleEvaluations(Long vehicleId, Integer current, Integer size) {
        long pageNo = current == null || current < 1 ? 1 : current;
        long pageSize = size == null || size < 1 ? 10 : Math.min(size, 50);

        Page<VehicleEvaluationItemVO> page = new Page<>(pageNo, pageSize);
        IPage<VehicleEvaluationItemVO> result = evaluationMapper.selectVehicleEvalPage(page, vehicleId);

        VehicleEvaluationVO summary = evaluationMapper.selectVehicleSummary(vehicleId);
        if (summary == null) {
            summary = new VehicleEvaluationVO();
        }
        summary.setRecords(result.getRecords());
        summary.setPage(result.getCurrent());
        summary.setSize(result.getSize());
        return summary;
    }

    /* ==================== 后台 / 网点 ==================== */

    public PageResult<EvaluationVO> page(EvaluationQueryDTO query) {
        Page<EvaluationVO> page = new Page<>(query.getCurrent(), query.getPageSize());
        IPage<EvaluationVO> result = evaluationMapper.selectEvalPage(page, query);
        result.getRecords().forEach(this::decorate);
        return PageResult.of(result);
    }

    /**
     * 统计
     *
     * @param storeId 非空时只统计该网点（网点工作台），null 为全平台（系统管理员）
     */
    public EvaluationStatsVO stats(Long storeId) {
        EvaluationStatsVO vo = evaluationMapper.selectStats(storeId);
        if (vo == null) {
            vo = new EvaluationStatsVO();
        }
        vo.setTotal(nz(vo.getTotal()));
        vo.setAvgRating(vo.getAvgRating() == null ? 0D : vo.getAvgRating());
        vo.setAvgVehicleRating(vo.getAvgVehicleRating() == null ? 0D : vo.getAvgVehicleRating());
        vo.setAvgStoreRating(vo.getAvgStoreRating() == null ? 0D : vo.getAvgStoreRating());
        vo.setFiveStar(nz(vo.getFiveStar()));
        vo.setFourStar(nz(vo.getFourStar()));
        vo.setLowStar(nz(vo.getLowStar()));
        vo.setReplied(nz(vo.getReplied()));
        vo.setUnreplied(nz(vo.getUnreplied()));
        return vo;
    }

    /** 回复评价（系统管理员视角，不限网点） */
    @Transactional(rollbackFor = Exception.class)
    public EvaluationVO reply(Long id, String reply) {
        if (evaluationMapper.selectById(id) == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "评价不存在");
        }
        return doReply(id, reply, "平台");
    }

    /**
     * 回复评价（网点管理员视角）
     *
     * <p>先确认这条评价确实属于本网点再回复。返回 404 而非 403：
     * 不告诉对方"这条评价存在、只是不归你"，避免被用来猜评价 ID。</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public EvaluationVO replyAsStore(Long id, String reply, Long storeId) {
        EvaluationVO exists = evaluationMapper.selectEvalById(id);
        if (exists == null || storeId == null || !storeId.equals(exists.getStoreId())) {
            throw new BusinessException(ResultCode.NOT_FOUND, "本网点不存在该评价");
        }
        return doReply(id, reply, "网点");
    }

    /** 展示 / 隐藏：评价本身没问题，只是不适合挂前台，随时可恢复 */
    @Transactional(rollbackFor = Exception.class)
    public EvaluationVO toggleStatus(Long id, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "状态只能是 0（隐藏）或 1（展示）");
        }
        OrderEvaluation exists = evaluationMapper.selectById(id);
        if (exists == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "评价不存在");
        }
        OrderEvaluation update = new OrderEvaluation();
        update.setId(id);
        update.setStatus(status);
        update.setUpdateTime(LocalDateTime.now());
        evaluationMapper.updateById(update);

        log.info("评价状态变更: id={}, {} -> {}", id, exists.getStatus(), status);
        return decorate(evaluationMapper.selectEvalById(id));
    }

    /**
     * 删除违规评价（仅系统管理员）
     *
     * <p>走逻辑删除而不是 DELETE：删了什么、谁删的，事后要能对上账。
     * 唯一索引 uk_order 仍占着 order_id，所以这条订单不会被重复评价——
     * 这正好符合预期：违规内容删掉，但"这单已经评过"的事实不变。</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public void remove(Long id, String reason) {
        OrderEvaluation exists = evaluationMapper.selectById(id);
        if (exists == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "评价不存在");
        }
        evaluationMapper.deleteById(id);
        log.warn("删除违规评价: id={}, orderId={}, 原因={}", id, exists.getOrderId(),
                StringUtils.hasText(reason) ? reason : "未填写");
    }

    /* ==================== 内部方法 ==================== */

    private EvaluationVO doReply(Long id, String reply, String operator) {
        if (!StringUtils.hasText(reply)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "回复内容不能为空");
        }
        OrderEvaluation update = new OrderEvaluation();
        update.setId(id);
        update.setReply(reply.trim());
        update.setReplyTime(LocalDateTime.now());
        update.setUpdateTime(LocalDateTime.now());
        evaluationMapper.updateById(update);

        log.info("{}回复评价: id={}", operator, id);
        return decorate(evaluationMapper.selectEvalById(id));
    }

    /** 综合分 = 两项平均四舍五入，不接受前端传，避免"两项 5 分、总分 1 分" */
    private static int avgRating(int vehicleRating, int storeRating) {
        return Math.round((vehicleRating + storeRating) / 2.0f);
    }

    private static String toJson(List<String> images) {
        if (images == null || images.isEmpty()) {
            return null;
        }
        try {
            return JSON.writeValueAsString(images);
        } catch (Exception e) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "图片地址格式不正确");
        }
    }

    private EvaluationVO decorate(EvaluationVO vo) {
        if (vo == null) {
            return null;
        }
        Integer rating = vo.getRating();
        if (rating != null && rating >= 1 && rating <= 5) {
            vo.setRatingLabel(RATING_LABELS.get(rating - 1));
        }
        vo.setStatusLabel(Integer.valueOf(1).equals(vo.getStatus()) ? "已展示" : "已隐藏");
        return vo;
    }

    private static int nz(Integer value) {
        return value == null ? 0 : value;
    }
}
