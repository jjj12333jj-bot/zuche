package com.zuche.module.store.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 平台网点列表项
 *
 * <p>与网点端 {@code StoreOverviewVO} 的区别：那份是「自己看自己」的经营数据，
 * 这份是「平台看全部」的资产与产出对比，所以带排名所需的车辆数、订单数、月营收。</p>
 */
@Data
@Schema(description = "平台网点列表项")
public class AdminStoreVO {

    private Long id;

    private String storeCode;

    private String storeName;

    private String address;

    private String phone;

    private String businessHours;

    @Schema(description = "仅存储，不做地图渲染")
    private BigDecimal longitude;

    private BigDecimal latitude;

    private Long managerId;

    @Schema(description = "网点负责人姓名")
    private String managerName;

    private Integer status;

    private String statusLabel;

    /* ===== 资产 ===== */

    private Integer vehicleTotal;

    private Integer vehicleAvailable;

    private Integer vehicleRented;

    private Integer vehicleMaintenance;

    /* ===== 经营 ===== */

    @Schema(description = "累计订单数（不含已取消）")
    private Integer orderTotal;

    @Schema(description = "进行中订单数")
    private Integer activeOrderCount;

    @Schema(description = "本月营收（不含待支付与已取消）")
    private BigDecimal monthRevenue;

    private LocalDateTime createTime;
}
