package com.zuche.module.dashboard.mapper;

import com.zuche.module.dashboard.dto.FunnelRawDTO;
import com.zuche.module.dashboard.vo.CreditLevelStatVO;
import com.zuche.module.dashboard.vo.KpiVO;
import com.zuche.module.dashboard.vo.ModelStatVO;
import com.zuche.module.dashboard.vo.NameValueVO;
import com.zuche.module.dashboard.vo.StoreStatVO;
import com.zuche.module.dashboard.vo.TrendPointVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * 运营看板统计
 *
 * <p>所有聚合都下推到数据库完成，不把明细捞到内存里再算。</p>
 */
@Mapper
public interface DashboardMapper {

    /** 指标卡数字（比率由 service 补算） */
    KpiVO selectKpi(@Param("startDate") LocalDate startDate);

    /** 每日营收趋势 */
    List<TrendPointVO> selectTrend(@Param("startDate") LocalDate startDate);

    /** 履约漏斗原始计数 */
    FunnelRawDTO selectFunnel(@Param("startDate") LocalDate startDate);

    /** 车辆状态分布 */
    List<NameValueVO> selectVehicleStatus();

    /** 网点经营 */
    List<StoreStatVO> selectStoreStats(@Param("startDate") LocalDate startDate);

    /** 车型营收排行 */
    List<ModelStatVO> selectModelStats(@Param("startDate") LocalDate startDate, @Param("limit") int limit);

    /** 信用等级分布 */
    List<CreditLevelStatVO> selectCreditLevels(@Param("startDate") LocalDate startDate);
}
