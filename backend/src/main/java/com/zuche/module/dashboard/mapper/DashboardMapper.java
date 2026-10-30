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

    /** 网点经营（utilStartDate 是车日利用率的窗口起点，可能比 startDate 晚） */
    List<StoreStatVO> selectStoreStats(@Param("startDate") LocalDate startDate,
                                       @Param("utilStartDate") LocalDate utilStartDate);

    /**
     * 窗口内被占用的车日合计（车日利用率的分子）
     *
     * <p>取消订单不占车；逾期单的车至今未还，占用截止到"今天"而不是计划还车日。</p>
     */
    Integer selectOccupiedVehicleDays(@Param("startDate") LocalDate startDate);

    /** 可租车辆数（排除下架与维保，作为车日利用率的分母基数） */
    Integer selectRentableVehicles();

    /** 平台首单日期，用于把统计窗口裁到"有数据的那一段" */
    LocalDate selectFirstOrderDate();

    /** 车型营收排行 */
    List<ModelStatVO> selectModelStats(@Param("startDate") LocalDate startDate, @Param("limit") int limit);

    /** 信用等级分布 */
    List<CreditLevelStatVO> selectCreditLevels(@Param("startDate") LocalDate startDate);
}
