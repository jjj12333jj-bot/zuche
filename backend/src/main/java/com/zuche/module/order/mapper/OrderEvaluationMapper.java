package com.zuche.module.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zuche.module.order.dto.EvaluationQueryDTO;
import com.zuche.module.order.entity.OrderEvaluation;
import com.zuche.module.order.vo.EvaluationStatsVO;
import com.zuche.module.order.vo.EvaluationVO;
import com.zuche.module.order.vo.VehicleEvaluationItemVO;
import com.zuche.module.order.vo.VehicleEvaluationVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 评价 Mapper
 */
@Mapper
public interface OrderEvaluationMapper extends BaseMapper<OrderEvaluation> {

    /** 评价分页：一次 JOIN 出订单号、网点、用户、车辆，避免每条再查四次 */
    IPage<EvaluationVO> selectEvalPage(Page<EvaluationVO> page, @Param("q") EvaluationQueryDTO query);

    /** 单条评价（回复后回传给前端，省一次整页刷新） */
    EvaluationVO selectEvalById(@Param("id") Long id);

    /** 按订单查评价（一单一评，最多一条） */
    EvaluationVO selectEvalByOrderId(@Param("orderId") Long orderId);

    /**
     * 统计
     *
     * @param storeId 非空时只统计该网点，网点工作台用；管理员传 null 看全平台
     */
    EvaluationStatsVO selectStats(@Param("storeId") Long storeId);

    /** 前台车辆评价分页：只查展示中的，评价人名称已脱敏 */
    IPage<VehicleEvaluationItemVO> selectVehicleEvalPage(Page<VehicleEvaluationItemVO> page,
                                                         @Param("vehicleId") Long vehicleId);

    /** 前台车辆评价摘要：平均分、双维度均分、星级分布 */
    VehicleEvaluationVO selectVehicleSummary(@Param("vehicleId") Long vehicleId);

    /** 批量查「已评价」的订单 ID，订单列表用它标记按钮，避免逐条查库（含已删除的评价） */
    List<Long> selectEvaluatedOrderIds(@Param("orderIds") List<Long> orderIds);

    /** 该订单是否已被评价过（含已删除的评价，用来判断还能不能点「发表评价」） */
    int countByOrderId(@Param("orderId") Long orderId);
}
