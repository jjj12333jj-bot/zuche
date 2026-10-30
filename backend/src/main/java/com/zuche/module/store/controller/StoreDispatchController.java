package com.zuche.module.store.controller;

import com.zuche.common.PageResult;
import com.zuche.common.Result;
import com.zuche.module.store.dto.DispatchCreateDTO;
import com.zuche.module.store.dto.DispatchQueryDTO;
import com.zuche.module.store.service.DispatchService;
import com.zuche.module.store.vo.DispatchVO;
import com.zuche.module.vehicle.vo.StoreOptionVO;
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

import java.util.List;

/**
 * 网点调车申请（仅网点管理员）
 *
 * <p>网点只负责「提申请」和「撤回」，审批权在平台（见 {@link AdminDispatchController}）。
 * 这样调车这条链上谁做了什么都能查到人，不会出现两个网点互相点头就把车搬走。</p>
 */
@Tag(name = "08-网点调车", description = "发起调车申请、撤回、查看审批结果")
@RestController
@RequestMapping("/api/store")
@RequiredArgsConstructor
@RequireRole(RoleCode.STORE_ADMIN)
@SecurityRequirement(name = "Authorization")
public class StoreDispatchController {

    private final DispatchService dispatchService;

    @Operation(summary = "调车申请分页",
            description = "direction=OUT 我调出的 / IN 调入我的 / 为空两个方向都看")
    @GetMapping("/dispatches")
    public Result<PageResult<DispatchVO>> dispatches(
            @ParameterObject @ModelAttribute DispatchQueryDTO query) {
        return Result.success(dispatchService.pageOfStore(query));
    }

    @Operation(summary = "发起调车申请",
            description = "只能申请调出本网点的可租车辆；同车同时只允许一条待审批申请")
    @PostMapping("/dispatches")
    public Result<Void> create(@Valid @RequestBody DispatchCreateDTO dto) {
        return Result.success(dispatchService.create(dto), null);
    }

    @Operation(summary = "撤回调车申请", description = "仅待审批状态可撤回；已通过的不能撤")
    @PostMapping("/dispatches/{id}/cancel")
    public Result<Void> cancel(@Parameter(description = "申请 ID") @PathVariable Long id) {
        return Result.success(dispatchService.cancel(id), null);
    }

    @Operation(summary = "可调入网点下拉", description = "全部网点去掉自己，供发起申请时选择")
    @GetMapping("/stores/options")
    public Result<List<StoreOptionVO>> stores() {
        return Result.success(dispatchService.otherStores());
    }
}
