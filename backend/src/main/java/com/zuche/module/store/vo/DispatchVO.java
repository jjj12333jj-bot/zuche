package com.zuche.module.store.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 调车申请列表项
 */
@Data
@Schema(description = "调车申请")
public class DispatchVO {

    private Long id;

    private String applyNo;

    private Long vehicleId;

    private String vehicleNo;

    private String licensePlate;

    private String modelName;

    private Long fromStoreId;

    private String fromStoreName;

    private Long toStoreId;

    private String toStoreName;

    private Long applyUserId;

    private String applyUserName;

    private String reason;

    private String status;

    private String statusLabel;

    private Long auditUserId;

    private String auditUserName;

    private LocalDateTime auditTime;

    private String auditRemark;

    private LocalDateTime createTime;

    /** 是否还能撤回：待审批且是本人网点发起的 */
    private Boolean canCancel;

    /** 是否还能审批：待审批 */
    private Boolean canAudit;
}
