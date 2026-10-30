package com.zuche.module.order.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zuche.common.BusinessException;
import com.zuche.common.PageResult;
import com.zuche.common.ResultCode;
import com.zuche.module.order.dto.EvaluationQueryDTO;
import com.zuche.module.order.entity.OrderEvaluation;
import com.zuche.module.order.mapper.OrderEvaluationMapper;
import com.zuche.module.order.vo.EvaluationStatsVO;
import com.zuche.module.order.vo.EvaluationVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 评价管理
 *
 * <p>评价不允许删除，只允许「隐藏」：差评被删掉，运营就失去了改进口碑的依据，
 * 而且用户会发现自己的评价凭空消失。隐藏保留原始内容，前台不展示，
 * 后台随时能查、能恢复。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EvaluationService {

    /** 星级文案：与前端评论文案保持一致，避免同一句话两处维护 */
    private static final List<String> RATING_LABELS =
            List.of("很不满意", "不满意", "一般", "满意", "非常满意");

    private final OrderEvaluationMapper evaluationMapper;

    public PageResult<EvaluationVO> page(EvaluationQueryDTO query) {
        Page<EvaluationVO> page = new Page<>(query.getCurrent(), query.getPageSize());
        IPage<EvaluationVO> result = evaluationMapper.selectEvalPage(page, query);
        result.getRecords().forEach(this::decorate);
        return PageResult.of(result);
    }

    public EvaluationStatsVO stats() {
        EvaluationStatsVO vo = evaluationMapper.selectStats();
        if (vo == null) {
            vo = new EvaluationStatsVO();
        }
        vo.setTotal(nz(vo.getTotal()));
        vo.setAvgRating(vo.getAvgRating() == null ? 0D : vo.getAvgRating());
        vo.setFiveStar(nz(vo.getFiveStar()));
        vo.setFourStar(nz(vo.getFourStar()));
        vo.setLowStar(nz(vo.getLowStar()));
        vo.setReplied(nz(vo.getReplied()));
        vo.setUnreplied(nz(vo.getUnreplied()));
        return vo;
    }

    /**
     * 回复评价
     *
     * <p>允许再次回复覆盖：第一条回复写错了不能改，运营只能再发一条，反而更乱。
     * 覆盖时更新 reply_time，前端据此显示「最后回复时间」。</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public EvaluationVO reply(Long id, String reply) {
        if (!StringUtils.hasText(reply)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "回复内容不能为空");
        }
        OrderEvaluation exists = evaluationMapper.selectById(id);
        if (exists == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "评价不存在");
        }
        OrderEvaluation update = new OrderEvaluation();
        update.setId(id);
        update.setReply(reply.trim());
        update.setReplyTime(LocalDateTime.now());
        update.setUpdateTime(LocalDateTime.now());
        evaluationMapper.updateById(update);

        log.info("回复评价: id={}, 是否首次={}", id, !StringUtils.hasText(exists.getReply()));
        return decorate(evaluationMapper.selectEvalById(id));
    }

    /** 展示 / 隐藏 */
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
