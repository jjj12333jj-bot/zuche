package com.zuche.module.store.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 调车审批意见
 *
 * <p>通过时选填，驳回时服务端强制必填——驳回不写理由，
 * 申请人只能反复重提，等于把沟通成本转嫁给一线。</p>
 */
@Data
@Schema(description = "调车审批意见")
public class DispatchAuditDTO {

    @Size(max = 200, message = "审批意见最多 200 字")
    private String remark;
}
