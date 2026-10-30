package com.zuche.module.dashboard.service;

import com.zuche.module.dashboard.dto.FunnelRawDTO;
import com.zuche.module.dashboard.mapper.DashboardMapper;
import com.zuche.module.dashboard.vo.CreditLevelStatVO;
import com.zuche.module.dashboard.vo.DashboardVO;
import com.zuche.module.dashboard.vo.FunnelStepVO;
import com.zuche.module.dashboard.vo.KpiVO;
import com.zuche.module.dashboard.vo.ModelStatVO;
import com.zuche.module.dashboard.vo.NameValueVO;
import com.zuche.module.dashboard.vo.StoreStatVO;
import com.zuche.module.dashboard.vo.TrendPointVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 运营看板统计
 *
 * <p>职责边界：SQL 只负责"捞数字"，比率、补齐日期、层级转化率都在这里算。
 * 这样口径调整时改一处即可，不用动 XML。</p>
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final int DEFAULT_DAYS = 30;
    private static final int MAX_DAYS = 180;
    private static final int MODEL_TOP = 8;

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final DashboardMapper dashboardMapper;

    public DashboardVO overview(Integer days) {
        int d = normalizeDays(days);
        // 含今天在内的 d 天窗口，例如 days=30 表示 [今天-29, 今天]
        LocalDate startDate = LocalDate.now().minusDays(d - 1L);

        DashboardVO vo = new DashboardVO();
        vo.setDays(d);
        vo.setGeneratedAt(LocalDateTime.now());

        vo.setKpi(buildKpi(dashboardMapper.selectKpi(startDate)));
        vo.setTrend(fillMissingDays(dashboardMapper.selectTrend(startDate), startDate, d));
        vo.setFunnel(buildFunnel(dashboardMapper.selectFunnel(startDate)));
        vo.setVehicleStatus(withRatio(dashboardMapper.selectVehicleStatus()));
        vo.setStores(buildStores(dashboardMapper.selectStoreStats(startDate)));
        vo.setModels(dashboardMapper.selectModelStats(startDate, MODEL_TOP));
        vo.setCreditLevels(buildCreditLevels(dashboardMapper.selectCreditLevels(startDate)));
        return vo;
    }

    private int normalizeDays(Integer days) {
        if (days == null || days <= 0) {
            return DEFAULT_DAYS;
        }
        return Math.min(days, MAX_DAYS);
    }

    /* ==================== 指标卡 ==================== */

    private KpiVO buildKpi(KpiVO kpi) {
        if (kpi == null) {
            return new KpiVO();
        }
        kpi.setCompleteRate(rate(kpi.getCompletedOrders(), kpi.getOrderTotal()));
        kpi.setRentRate(rate(kpi.getVehicleRented(), kpi.getVehicleTotal()));
        return kpi;
    }

    /* ==================== 趋势 ==================== */

    /**
     * 补齐没有订单的日期
     *
     * <p>SQL 只会返回有支付记录的日子，直接丢给图表会出现"日期断裂"，
     * 折线看起来像漏了几天。这里按窗口生成连续日期，缺的补 0。</p>
     */
    private List<TrendPointVO> fillMissingDays(List<TrendPointVO> rows, LocalDate startDate, int days) {
        Map<String, TrendPointVO> indexed = new LinkedHashMap<>();
        if (rows != null) {
            for (TrendPointVO row : rows) {
                if (row.getDate() != null) {
                    indexed.put(row.getDate(), row);
                }
            }
        }

        List<TrendPointVO> result = new ArrayList<>(days);
        for (int i = 0; i < days; i++) {
            String key = startDate.plusDays(i).format(DAY);
            TrendPointVO point = indexed.get(key);
            if (point == null) {
                point = new TrendPointVO();
                point.setDate(key);
                point.setRentAmount(BigDecimal.ZERO);
                point.setOrderCount(0);
                point.setLateFee(BigDecimal.ZERO);
            }
            if (point.getRentAmount() == null) {
                point.setRentAmount(BigDecimal.ZERO);
            }
            if (point.getLateFee() == null) {
                point.setLateFee(BigDecimal.ZERO);
            }
            if (point.getOrderCount() == null) {
                point.setOrderCount(0);
            }
            result.add(point);
        }
        return result;
    }

    /* ==================== 漏斗 ==================== */

    private List<FunnelStepVO> buildFunnel(FunnelRawDTO raw) {
        List<FunnelStepVO> steps = new ArrayList<>(5);
        if (raw == null) {
            return steps;
        }
        int base = nz(raw.getOrderTotal());
        steps.add(step("下单", base, base, base));
        steps.add(step("支付", nz(raw.getPaid()), base, base));
        steps.add(step("取车", nz(raw.getPicked()), nz(raw.getPaid()), base));
        steps.add(step("归还", nz(raw.getReturned()), nz(raw.getPicked()), base));
        steps.add(step("完成", nz(raw.getCompleted()), nz(raw.getReturned()), base));
        return steps;
    }

    private FunnelStepVO step(String stage, int count, int prevCount, int baseCount) {
        FunnelStepVO vo = new FunnelStepVO();
        vo.setStage(stage);
        vo.setCount(count);
        Double r = rate(count, prevCount);
        vo.setRate(r);
        vo.setDropRate(r == null ? null : round(100d - r));
        vo.setOverallRate(rate(count, baseCount));
        return vo;
    }

    /* ==================== 车辆状态 ==================== */

    private List<NameValueVO> withRatio(List<NameValueVO> list) {
        if (list == null || list.isEmpty()) {
            return new ArrayList<>();
        }
        int total = list.stream().mapToInt(x -> nz(x.getValue())).sum();
        for (NameValueVO item : list) {
            item.setRatio(rate(nz(item.getValue()), total));
        }
        return list;
    }

    /* ==================== 网点 ==================== */

    private List<StoreStatVO> buildStores(List<StoreStatVO> list) {
        if (list == null) {
            return new ArrayList<>();
        }
        for (StoreStatVO s : list) {
            s.setCompleteRate(rate(s.getCompletedCount(), s.getOrderCount()));
            int vehicles = nz(s.getVehicleCount());
            if (vehicles > 0 && s.getRentAmount() != null) {
                s.setPerVehicle(s.getRentAmount()
                        .divide(BigDecimal.valueOf(vehicles), 2, RoundingMode.HALF_UP));
            } else {
                s.setPerVehicle(BigDecimal.ZERO);
            }
        }
        return list;
    }

    /* ==================== 信用分布 ==================== */

    private List<CreditLevelStatVO> buildCreditLevels(List<CreditLevelStatVO> list) {
        if (list == null) {
            return new ArrayList<>();
        }
        int totalUsers = list.stream().mapToInt(x -> nz(x.getUserCount())).sum();
        for (CreditLevelStatVO c : list) {
            c.setUserRatio(rate(nz(c.getUserCount()), totalUsers));
            c.setOverdueRate(rate(nz(c.getOverdueCount()), nz(c.getOrderCount())));
            if (c.getAvgScore() == null) {
                c.setAvgScore(BigDecimal.ZERO);
            }
            if (c.getRentAmount() == null) {
                c.setRentAmount(BigDecimal.ZERO);
            }
        }
        return list;
    }

    /* ==================== 工具 ==================== */

    private static int nz(Integer v) {
        return v == null ? 0 : v;
    }

    /** 百分比，保留 1 位；分母为 0 返回 null 由前端显示 "—" */
    private static Double rate(int numerator, int denominator) {
        if (denominator <= 0) {
            return null;
        }
        return round(numerator * 100d / denominator);
    }

    private static Double round(double v) {
        return BigDecimal.valueOf(v).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }
}
