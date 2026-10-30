package com.zuche.module.store.controller;

import com.zuche.common.PageResult;
import com.zuche.common.Result;
import com.zuche.module.store.dto.DispatchAuditDTO;
import com.zuche.module.store.dto.DispatchQueryDTO;
import com.zuche.module.store.service.DispatchService;
import com.zuche.module.store.vo.DispatchVO;
import com.zuche.security.RequireRole;
import com.zuche.security.RoleCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 调车审批（仅平台管理员）
 *
 * <p>审批通过不只是改一个状态字段——车辆归属要真正迁移到调入网点，
 * 迁移失败（车被租走、已被别的申请调走）时整笔审批回滚。
 * 放在一个事务里，是为了避免「申请显示已通过但车没动」这种两头对不上的中间态。</p>
 */
@Tag(name = "09-调车审批", description = "平台侧调车审批：查看全部申请、通过、驳回")
@RestController
@RequestMapping("/api/admin/dispatch")
@RequiredArgsConstructor
@RequireRole(RoleCode.SYS_ADMIN)
@SecurityRequirement(name = "Authorization")
public class AdminDispatchController {

    private final DispatchService dispatchService;

    @Operation(summary = "调车申请分页", description = "平台视角，可看全部网点申请，按状态与关键词筛选")
    @GetMapping
    public Result<PageResult<DispatchVO>> page(
            @ParameterObject @ModelAttribute DispatchQueryDTO query) {
        return Result.success(dispatchService.pageOfAdmin(query));
    }

    @Operation(summary = "审批通过", description = "车辆归属迁移至调入网点；车辆状态已变化时整笔拒绝")
    @PostMapping("/{id}/approve")
    public Result<Void> approve(@Parameter(description = "申请 ID") @PathVariable Long id,
                                @Valid @RequestBody(required = false) DispatchAuditDTO dto) {
        return Result.success(dispatchService.approve(id, dto), null);
    }

    @Operation(summary = "审批驳回", description = "驳回原因必填")
    @PostMapping("/{id}/reject")
    public Result<Void> reject(@Parameter(description = "申请 ID") @PathVariable Long id,
                               @Valid @RequestBody(required = false) DispatchAuditDTO dto) {
        return Result.success(dispatchService.reject(id, dto), null);
    }
}
