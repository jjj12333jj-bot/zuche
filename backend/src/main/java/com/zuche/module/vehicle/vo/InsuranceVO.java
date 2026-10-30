package com.zuche.module.vehicle.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 保险记录
 *
 * <p>daysToExpire 由 SQL 用 DATEDIFF 算出来而不是前端拿 endDate 减今天：
 * 前端时区、夏令时、以及「今天」的边界都容易算错，放在一处算更可靠。</p>
 */
@Data
@Schema(description = "保险记录")
public class InsuranceVO {

    private Long id;

    private Long vehicleId;

    private String vehicleNo;

    private String licensePlate;

    private String brandName;

    private String modelName;

    private Long storeId;

    private String storeName;

    private String insuranceType;

    private String typeLabel;

    private String insurer;

    private String policyNo;

    private LocalDate startDate;

    private LocalDate endDate;

    private BigDecimal premium;

    private Integer status;

    private String statusLabel;

    @Schema(description = "距到期天数，负数表示已过期")
    private Integer daysToExpire;

    private LocalDateTime createTime;
}
