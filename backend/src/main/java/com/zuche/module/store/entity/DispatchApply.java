package com.zuche.module.store.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 调车申请（对应 dispatch_apply）
 *
 * <p>方向语义固定：{@code from_store_id} 是车辆当前归属网点（调出方），
 * {@code to_store_id} 是希望调入的网点。申请由调出方发起——
 * 车在谁名下就由谁申请，平台审批通过后车辆归属才迁移。
 * 这样「谁能把车调走」永远只有一个答案，不会出现两个网点互相同意就绕过平台。</p>
 */
@Data
@TableName("dispatch_apply")
@Schema(description = "调车申请")
public class DispatchApply {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 申请单号 D + yyyyMMdd + 4 位流水 */
    private String applyNo;

    private Long vehicleId;

    /** 调出网点（车辆当前归属） */
    private Long fromStoreId;

    /** 调入网点 */
    private Long toStoreId;

    private Long applyUserId;

    private String reason;

    /** PENDING 待审批 / APPROVED 通过 / REJECTED 驳回 / CANCELLED 撤回 */
    private String status;

    private Long auditUserId;

    private LocalDateTime auditTime;

    private String auditRemark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
