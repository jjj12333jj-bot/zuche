package com.zuche.module.vehicle.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 车辆资产统计（全量口径）
 *
 * <p>这些数字必须由库里聚合，不能用「当前页 10 条」去算：
 * 保险 48 张分 5 页，按页算「30 天内到期」只会得到第一页的 6 张，
 * 看起来对、翻页就错——答辩时被问一句「为什么翻页数字变了」就说不清。</p>
 */
@Data
@Schema(description = "车辆资产统计")
public class AdminVehicleStatsVO {

    private BrandStats brand;

    private MaintStats maintenance;

    private InsuranceStats insurance;

    @Data
    @Schema(description = "品牌统计")
    public static class BrandStats {
        private Integer total;
        private Integer enabled;
        private Integer disabled;
        private Integer vehicles;
        private Integer rentable;
    }

    @Data
    @Schema(description = "保养维修统计")
    public static class MaintStats {
        private Integer total;
        private Integer maintain;
        private Integer repair;
        private BigDecimal cost;
    }

    @Data
    @Schema(description = "保险统计")
    public static class InsuranceStats {
        private Integer total;
        /** 已过期但仍在册（status=1） */
        private Integer expired;
        /** 30 天内到期（含今天，不含已过期） */
        private Integer soon;
        private BigDecimal premium;
    }
}
