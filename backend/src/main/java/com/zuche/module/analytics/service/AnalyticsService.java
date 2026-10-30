package com.zuche.module.analytics.service;

import com.zuche.module.analytics.mapper.AnalyticsMapper;
import com.zuche.module.analytics.vo.ForecastPointVO;
import com.zuche.module.analytics.vo.HeatmapCellVO;
import com.zuche.module.analytics.vo.ModelPreferenceVO;
import com.zuche.module.analytics.vo.OrderHeatmapVO;
import com.zuche.module.analytics.vo.RevenueForecastVO;
import com.zuche.module.dashboard.vo.TrendPointVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 智能分析
 *
 * <p>三块内容：下单时段热力、车型偏好矩阵、营收时序预测。
 * 前两块是"看清已经发生了什么"，第三块是"猜接下来会怎样"。
 * 所有比率与预测都在这里算，SQL 只负责把原始数字捞回来。</p>
 */
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private static final int HEATMAP_DAYS = 90;
    private static final int MAX_HEATMAP_DAYS = 365;

    private static final int TREND_DAYS = 45;
    private static final int MAX_TREND_DAYS = 120;
    private static final int HORIZON = 7;
    private static final int MAX_HORIZON = 14;

    /** 季节性朴素模型的同星期样本数：最近 4 周同一星期几 */
    private static final int SEASON_WEEKS = 4;
    /** 回测窗口：最近 21 天做滚动一步预测 */
    private static final int BACKTEST_DAYS = 21;
    /** 80% 双侧置信区间对应的正态分位数 */
    private static final double Z80 = 1.28;

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final AnalyticsMapper analyticsMapper;

    /* ==================== 下单时段热力 ==================== */

    public OrderHeatmapVO heatmap(Integer days) {
        int d = clamp(days, HEATMAP_DAYS, MAX_HEATMAP_DAYS);
        LocalDate startDate = LocalDate.now().minusDays(d - 1L);

        List<HeatmapCellVO> cells = analyticsMapper.selectOrderHeatmap(startDate);
        if (cells == null) {
            cells = new ArrayList<>();
        }

        int[] byWeekday = new int[7];   // 0=周一
        int[] byHour = new int[24];
        int peakCount = 0;
        Integer peakWeekday = null;
        Integer peakHour = null;
        int total = 0;
        BigDecimal totalRent = BigDecimal.ZERO;

        for (HeatmapCellVO cell : cells) {
            int wd = cell.getWeekday() == null ? 0 : cell.getWeekday();
            int hh = cell.getHour() == null ? 0 : cell.getHour();
            int cnt = cell.getOrderCount() == null ? 0 : cell.getOrderCount();
            total += cnt;
            if (cell.getRentAmount() != null) {
                totalRent = totalRent.add(cell.getRentAmount());
            }
            if (wd >= 1 && wd <= 7) {
                byWeekday[wd - 1] += cnt;
            }
            if (hh >= 0 && hh < 24) {
                byHour[hh] += cnt;
            }
            if (cnt > peakCount) {
                peakCount = cnt;
                peakWeekday = wd;
                peakHour = hh;
            }
        }

        OrderHeatmapVO vo = new OrderHeatmapVO();
        vo.setDays(d);
        vo.setTotalOrders(total);
        vo.setTotalRent(totalRent.setScale(2, RoundingMode.HALF_UP));
        vo.setCells(cells);
        vo.setPeakWeekday(peakWeekday);
        vo.setPeakHour(peakHour);
        vo.setPeakCount(peakCount);
        vo.setWeekdayTotals(toList(byWeekday));
        vo.setHourTotals(toList(byHour));
        vo.setBusiestHour(argMax(byHour));
        Integer busiestWeekday = argMax(byWeekday);
        vo.setBusiestWeekday(busiestWeekday == null ? null : busiestWeekday + 1);
        return vo;
    }

    /* ==================== 车型偏好 ==================== */

    public List<ModelPreferenceVO> modelPreference(Integer days) {
        int d = clamp(days, HEATMAP_DAYS, MAX_HEATMAP_DAYS);
        LocalDate startDate = LocalDate.now().minusDays(d - 1L);

        List<ModelPreferenceVO> list = analyticsMapper.selectModelPreference(startDate);
        if (list == null || list.isEmpty()) {
            return new ArrayList<>();
        }

        int totalOrders = list.stream().mapToInt(x -> nz(x.getOrderCount())).sum();
        BigDecimal totalRent = list.stream()
                .map(x -> x.getRentAmount() == null ? BigDecimal.ZERO : x.getRentAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        double weeks = Math.max(d / 7.0, 1.0);
        for (ModelPreferenceVO vo : list) {
            vo.setOrderRatio(ratio(nz(vo.getOrderCount()), totalOrders));
            BigDecimal rent = vo.getRentAmount() == null ? BigDecimal.ZERO : vo.getRentAmount();
            vo.setRentRatio(rentRatio(rent, totalRent));
            int vehicles = nz(vo.getVehicleCount());
            // 单车单周产出：同一分类里 5 台车和 1 台车没有可比性，必须摊到每台车每周
            if (vehicles > 0) {
                vo.setRentPerVehicleWeek(rent
                        .divide(BigDecimal.valueOf(vehicles), 2, RoundingMode.HALF_UP)
                        .divide(BigDecimal.valueOf(weeks), 2, RoundingMode.HALF_UP));
            } else {
                vo.setRentPerVehicleWeek(BigDecimal.ZERO);
            }
            if (vo.getAvgOrderAmount() == null) {
                vo.setAvgOrderAmount(BigDecimal.ZERO);
            }
            if (vo.getAvgRentDays() == null) {
                vo.setAvgRentDays(BigDecimal.ZERO);
            }
            if (vo.getAvgDailyPrice() == null) {
                vo.setAvgDailyPrice(BigDecimal.ZERO);
            }
        }
        return list;
    }

    /* ==================== 营收时序预测 ==================== */

    /**
     * 营收预测
     *
     * <p>用季节性朴素模型而不是 ARIMA：日粒度样本只有几十条，ARIMA 的参数估不准，
     * 答辩时也讲不清。这个模型三句话能讲完——</p>
     * <pre>
     *   1. 基准值 = 最近 4 周同一个星期几的平均营收
     *   2. 趋势修正 = 近 7 天均值 ÷ 前 21 天均值，向 1 收缩一半，限幅 [0.75, 1.35]
     *   3. 区间 = 回测残差标准差 × 1.28（80% 置信），随预测步长放大
     * </pre>
     */
    public RevenueForecastVO forecast(Integer days, Integer horizon) {
        int d = clamp(days, TREND_DAYS, MAX_TREND_DAYS);
        int h = clamp(horizon, HORIZON, MAX_HORIZON);

        LocalDate today = LocalDate.now();
        LocalDate firstPaid = analyticsMapper.selectFirstPaidDate();

        // 窗口不能早于首单日，否则前面全是零，会把均值拖垮
        LocalDate startDate = today.minusDays(d - 1L);
        if (firstPaid != null && firstPaid.isAfter(startDate)) {
            startDate = firstPaid;
        }
        int span = (int) (today.toEpochDay() - startDate.toEpochDay()) + 1;
        span = Math.max(span, 1);

        double[] series = buildSeries(analyticsMapper.selectDailyRent(startDate), startDate, span);

        RevenueForecastVO vo = new RevenueForecastVO();
        vo.setHistoryDays(span);
        vo.setHorizon(h);

        // 历史点
        List<TrendPointVO> history = new ArrayList<>(span);
        for (int i = 0; i < span; i++) {
            TrendPointVO p = new TrendPointVO();
            p.setDate(startDate.plusDays(i).format(DAY));
            p.setRentAmount(BigDecimal.valueOf(series[i]).setScale(2, RoundingMode.HALF_UP));
            p.setOrderCount(0);
            p.setLateFee(BigDecimal.ZERO);
            history.add(p);
        }
        vo.setHistory(history);

        Backtest bt = backtest(series, startDate);

        // 未来预测
        List<ForecastPointVO> forecast = new ArrayList<>(h);
        BigDecimal total = BigDecimal.ZERO;
        double sigma = bt.rmse > 0 ? bt.rmse : stdDev(series);
        for (int step = 1; step <= h; step++) {
            int idx = span - 1 + step;
            LocalDate date = startDate.plusDays(idx);
            double base = weekdayBase(series, startDate, idx);
            double value = base * trendFactor(series);
            // 步长越长越不确定：方差按 sqrt(1 + step/14) 放大
            double band = Z80 * sigma * Math.sqrt(1 + step / 14.0);

            ForecastPointVO fp = new ForecastPointVO();
            fp.setDate(date.format(DAY));
            fp.setWeekday(date.getDayOfWeek().getValue());
            fp.setValue(round(Math.max(0, value)));
            fp.setLower(round(Math.max(0, value - band)));
            fp.setUpper(round(value + band));
            fp.setWeekdayBase(round(base));
            forecast.add(fp);
            total = total.add(fp.getValue());
        }
        vo.setForecast(forecast);

        /* ---------- 指标 ---------- */

        int recentWin = Math.min(28, span);
        double recentMean = mean(series, span - recentWin, span);
        double last7 = span >= 7 ? sum(series, span - 7, span) : recentMean * 7;

        vo.setRecentDailyAvg(round(recentMean));
        vo.setLastWeekTotal(round(last7));
        vo.setForecastTotal(total.setScale(2, RoundingMode.HALF_UP));
        vo.setForecastDailyAvg(round(total.doubleValue() / h));
        vo.setTrendSlope(round(slope(series)));
        vo.setBacktestDays(bt.count);
        vo.setMae(round(bt.mae));
        vo.setMape(bt.mape);
        vo.setCoverage(bt.coverage);
        vo.setWeeklyWape(bt.weeklyWape);
        vo.setR2(bt.r2);

        double predictedAvg = total.doubleValue() / h;
        vo.setVsRecentAvg(recentMean > 0
                ? round2((predictedAvg - recentMean) / recentMean * 100)
                : null);

        String confidence;
        String label;
        if (bt.weeklyWape == null) {
            confidence = "LOW";
            label = "样本不足";
        } else if (bt.weeklyWape < 12) {
            confidence = "HIGH";
            label = "较高";
        } else if (bt.weeklyWape < 25) {
            confidence = "MEDIUM";
            label = "中等";
        } else {
            confidence = "LOW";
            label = "偏低";
        }
        vo.setConfidence(confidence);
        vo.setConfidenceLabel(label);
        vo.setMethod("季节性朴素模型：预测值 = 近 " + SEASON_WEEKS
                + " 周同一星期几的均值 × 趋势修正系数；区间按回测残差 ±1.28σ（80% 置信），"
                + "并随预测步长放宽。误差指标来自最近 " + BACKTEST_DAYS + " 天的滚动回测。");
        return vo;
    }

    /* ==================== 预测内部实现 ==================== */

    private double[] buildSeries(List<TrendPointVO> rows, LocalDate startDate, int span) {
        Map<String, Double> indexed = new HashMap<>();
        if (rows != null) {
            for (TrendPointVO row : rows) {
                if (row.getDate() != null && row.getRentAmount() != null) {
                    indexed.put(row.getDate(), row.getRentAmount().doubleValue());
                }
            }
        }
        double[] series = new double[span];
        for (int i = 0; i < span; i++) {
            series[i] = indexed.getOrDefault(startDate.plusDays(i).format(DAY), 0d);
        }
        return series;
    }

    /** 同星期基准值：最近 4 周同一个星期几的均值，并对极端值做夹逼 */
    private double weekdayBase(double[] s, LocalDate firstDate, int targetIdx) {
        int n = s.length;
        double recentMean = mean(s, Math.max(0, n - 28), n);
        double sum = 0;
        int cnt = 0;
        for (int k = 1; k <= SEASON_WEEKS; k++) {
            int idx = targetIdx - 7 * k;
            if (idx >= 0 && idx < n) {
                sum += s[idx];
                cnt++;
            }
        }
        double base = cnt > 0 ? sum / cnt : recentMean;
        if (recentMean <= 0) {
            return 0;
        }
        // 单个星期几可能被一笔大单拉高（例如周四租出一台 5 系），夹到均值的 0.4~2 倍
        return Math.min(Math.max(base, recentMean * 0.4), recentMean * 2.0);
    }

    /** 趋势修正系数：近 7 天相对前 21 天的变化，向 1 收缩一半，限幅避免外推失控 */
    private double trendFactor(double[] s) {
        int n = s.length;
        if (n < 21) {
            return 1d;
        }
        double last7 = mean(s, n - 7, n);
        // 回测早期历史可能不足 28 天，起点要夹到 0，否则 n-28 会变成负下标
        double prev = mean(s, Math.max(0, n - 28), n - 7);
        if (prev <= 0) {
            return 1d;
        }
        double ratio = Math.min(Math.max(last7 / prev, 0.75), 1.35);
        // 只信一半：日营收噪声大，完全采信趋势会把预测甩出去
        return 1 + (ratio - 1) * 0.5;
    }

    /**
     * 滚动回测
     *
     * <p>一步回测算 MAE/MAPE/R²，七步回测算周合计 WAPE。
     * 七步回测更重要：业务上问的是"下周大概多少"，日粒度误差大但周合计会互相抵消。</p>
     */
    private Backtest backtest(double[] series, LocalDate firstDate) {
        Backtest bt = new Backtest();
        int n = series.length;
        if (n < 15) {
            return bt;
        }

        List<Double> actuals = new ArrayList<>();
        List<Double> preds = new ArrayList<>();
        int from = Math.max(14, n - BACKTEST_DAYS);
        for (int i = from; i < n; i++) {
            double p = predictAt(series, firstDate, i - 1, i);
            actuals.add(series[i]);
            preds.add(p);
        }
        bt.count = actuals.size();
        if (bt.count > 0) {
            double absSum = 0;
            double sqSum = 0;
            double mapeSum = 0;
            int mapeCnt = 0;
            double meanActual = actuals.stream().mapToDouble(Double::doubleValue).average().orElse(0);
            double ssTot = 0;
            for (int i = 0; i < bt.count; i++) {
                double a = actuals.get(i);
                double p = preds.get(i);
                double e = Math.abs(a - p);
                absSum += e;
                sqSum += e * e;
                ssTot += (a - meanActual) * (a - meanActual);
                if (a > 0) {
                    mapeSum += e / a;
                    mapeCnt++;
                }
            }
            bt.mae = absSum / bt.count;
            bt.rmse = Math.sqrt(sqSum / bt.count);
            bt.mape = mapeCnt > 0 ? round2(mapeSum / mapeCnt * 100) : null;
            bt.r2 = ssTot > 0 ? round3(1 - (sqSum / ssTot)) : null;
            // 区间覆盖率：残差落在 ±1.28σ 内的比例。名义值是 80%，
            // 明显低于 80% 说明残差是厚尾的，区间要按实际覆盖率打折看
            int hit = 0;
            double band = Z80 * bt.rmse;
            for (int i = 0; i < bt.count; i++) {
                if (Math.abs(actuals.get(i) - preds.get(i)) <= band) {
                    hit++;
                }
            }
            bt.coverage = round2(hit * 100d / bt.count);
        }

        // 七步回测：每个起点预测未来 7 天合计，与实际 7 天合计比
        double errSum = 0;
        double actSum = 0;
        int origins = 0;
        for (int i = Math.max(14, n - 21); i <= n - 7; i++) {
            double pred7 = 0;
            double actual7 = 0;
            for (int k = 0; k < 7; k++) {
                pred7 += predictAt(series, firstDate, i - 1, i + k);
                actual7 += series[i + k];
            }
            errSum += Math.abs(actual7 - pred7);
            actSum += actual7;
            origins++;
        }
        if (origins > 0 && actSum > 0) {
            bt.weeklyWape = round2(errSum / actSum * 100);
        }
        return bt;
    }

    /**
     * 用 [0, origin] 这段历史预测第 target 天
     *
     * <p>回测必须严格只用"当时能看到的数据"，否则就是拿未来预测过去，指标会假好。</p>
     */
    private double predictAt(double[] full, LocalDate firstDate, int origin, int target) {
        double[] hist = Arrays.copyOfRange(full, 0, origin + 1);
        double base = weekdayBase(hist, firstDate, target);
        return Math.max(0, base * trendFactor(hist));
    }

    /* ==================== 工具 ==================== */

    private static int clamp(Integer v, int def, int max) {
        if (v == null || v <= 0) {
            return def;
        }
        return Math.min(v, max);
    }

    private static int nz(Integer v) {
        return v == null ? 0 : v;
    }

    private static double mean(double[] s, int from, int to) {
        int lo = Math.max(0, Math.min(from, s.length));
        int hi = Math.max(lo, Math.min(to, s.length));
        if (hi <= lo) {
            return 0;
        }
        double sum = 0;
        for (int i = lo; i < hi; i++) {
            sum += s[i];
        }
        return sum / (hi - lo);
    }

    private static double sum(double[] s, int from, int to) {
        double sum = 0;
        for (int i = from; i < to; i++) {
            sum += s[i];
        }
        return sum;
    }

    private static double stdDev(double[] s) {
        if (s.length < 2) {
            return 0;
        }
        double m = mean(s, 0, s.length);
        double sq = 0;
        for (double v : s) {
            sq += (v - m) * (v - m);
        }
        return Math.sqrt(sq / (s.length - 1));
    }

    /** 最近 14 天的最小二乘斜率，仅用于展示"在涨还是在跌" */
    private static double slope(double[] s) {
        int n = s.length;
        int m = Math.min(14, n);
        if (m < 2) {
            return 0;
        }
        int from = n - m;
        double mx = (m - 1) / 2.0;
        double my = mean(s, from, n);
        double num = 0;
        double den = 0;
        for (int i = 0; i < m; i++) {
            num += (i - mx) * (s[from + i] - my);
            den += (i - mx) * (i - mx);
        }
        return den == 0 ? 0 : num / den;
    }

    private static Double ratio(int num, int den) {
        if (den <= 0) {
            return null;
        }
        return round2(num * 100d / den);
    }

    private static Double rentRatio(BigDecimal part, BigDecimal total) {
        if (total == null || total.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        return part.multiply(BigDecimal.valueOf(100))
                .divide(total, 2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private static BigDecimal round(double v) {
        return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP);
    }

    private static Double round2(double v) {
        return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private static Double round3(double v) {
        return BigDecimal.valueOf(v).setScale(3, RoundingMode.HALF_UP).doubleValue();
    }

    private static List<Integer> toList(int[] arr) {
        List<Integer> list = new ArrayList<>(arr.length);
        for (int v : arr) {
            list.add(v);
        }
        return list;
    }

    private static Integer argMax(int[] arr) {
        int best = 0;
        int bestIdx = -1;
        for (int i = 0; i < arr.length; i++) {
            if (arr[i] > best) {
                best = arr[i];
                bestIdx = i;
            }
        }
        return bestIdx < 0 ? null : bestIdx;
    }

    /** 回测结果容器 */
    private static class Backtest {
        private int count;
        private double mae;
        private double rmse;
        private Double mape;
        private Double coverage;
        private Double weeklyWape;
        private Double r2;
    }
}
