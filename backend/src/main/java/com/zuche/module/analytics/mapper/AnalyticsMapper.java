package com.zuche.module.analytics.mapper;

import com.zuche.module.analytics.vo.HeatmapCellVO;
import com.zuche.module.analytics.vo.ModelPreferenceVO;
import com.zuche.module.dashboard.vo.TrendPointVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * 智能分析取数
 *
 * <p>只做聚合，不做计算：预测、比率、区间都在 service 里算，
 * 这样换模型不用改 SQL，换口径不用改算法。</p>
 */
@Mapper
public interface AnalyticsMapper {

    /**
     * 下单时段分布：星期 × 小时
     *
     * <p>用下单时间而非取车时间。取车时间在种子数据里是按取车日 9:00 生成的，
     * 画出来只有一条竖线；下单时间才是用户真实行为，也才有双峰形态。</p>
     */
    List<HeatmapCellVO> selectOrderHeatmap(@Param("startDate") LocalDate startDate);

    /** 车型分类偏好：订单量、客单价、营收、在册车辆数 */
    List<ModelPreferenceVO> selectModelPreference(@Param("startDate") LocalDate startDate);

    /** 每日租金实收（支付口径），只返回有支付的日子 */
    List<TrendPointVO> selectDailyRent(@Param("startDate") LocalDate startDate);

    /** 数据里最早的一天，用于把历史窗口裁到有数据的区间 */
    LocalDate selectFirstPaidDate();
}
