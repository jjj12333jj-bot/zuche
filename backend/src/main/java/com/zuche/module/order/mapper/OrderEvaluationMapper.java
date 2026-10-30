package com.zuche.module.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zuche.module.order.dto.EvaluationQueryDTO;
import com.zuche.module.order.entity.OrderEvaluation;
import com.zuche.module.order.vo.EvaluationStatsVO;
import com.zuche.module.order.vo.EvaluationVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 评价 Mapper
 */
@Mapper
public interface OrderEvaluationMapper extends BaseMapper<OrderEvaluation> {

    /** 评价分页：一次 JOIN 出订单号、网点、用户、车辆，避免每条再查四次 */
    IPage<EvaluationVO> selectEvalPage(Page<EvaluationVO> page, @Param("q") EvaluationQueryDTO query);

    /** 单条评价（回复后回传给前端，省一次整页刷新） */
    EvaluationVO selectEvalById(@Param("id") Long id);

    /** 全量统计 */
    EvaluationStatsVO selectStats();
}
