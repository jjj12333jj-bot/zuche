package com.zuche.module.store.controller;

import com.zuche.common.PageResult;
import com.zuche.common.Result;
import com.zuche.common.StatusDTO;
import com.zuche.module.store.dto.AdminStoreQueryDTO;
import com.zuche.module.store.dto.StoreSaveDTO;
import com.zuche.module.store.service.AdminStoreService;
import com.zuche.module.store.vo.AdminStoreStatsVO;
import com.zuche.module.store.vo.AdminStoreVO;
import com.zuche.security.RequireRole;
import com.zuche.security.RoleCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 平台网点管理（仅系统管理员）
 */
@Tag(name = "13-网点管理", description = "网点查询、新增、编辑、停用、删除")
@RestController
@RequestMapping("/api/admin/stores")
@RequiredArgsConstructor
@RequireRole(RoleCode.SYS_ADMIN)
@SecurityRequirement(name = "Authorization")
public class AdminStoreController {

    private final AdminStoreService adminStoreService;

    @Operation(summary = "网点统计", description = "总数 / 营业 / 停用 / 车辆总数 / 本月营收")
    @GetMapping("/stats")
    public Result<AdminStoreStatsVO> stats() {
        return Result.success(adminStoreService.stats());
    }

    @Operation(summary = "网点分页", description = "带车辆数、订单数、本月营收")
    @GetMapping
    public Result<PageResult<AdminStoreVO>> page(@ParameterObject @ModelAttribute AdminStoreQueryDTO query) {
        return Result.success(adminStoreService.page(query));
    }

    @Operation(summary = "网点详情")
    @GetMapping("/{id}")
    public Result<AdminStoreVO> detail(@Parameter(description = "网点 ID") @PathVariable Long id) {
        return Result.success(adminStoreService.detail(id));
    }

    @Operation(summary = "新增网点", description = "编码唯一，格式 2~10 位大写字母或数字")
    @PostMapping
    public Result<AdminStoreVO> create(@Valid @RequestBody StoreSaveDTO dto) {
        return Result.success("网点已创建", adminStoreService.create(dto));
    }

    @Operation(summary = "编辑网点")
    @PutMapping("/{id}")
    public Result<AdminStoreVO> update(@Parameter(description = "网点 ID") @PathVariable Long id,
                                       @Valid @RequestBody StoreSaveDTO dto) {
        return Result.success("网点已更新", adminStoreService.update(id, dto));
    }

    @Operation(summary = "停用/恢复营业", description = "有进行中订单时不允许停用")
    @PutMapping("/{id}/status")
    public Result<AdminStoreVO> status(@Parameter(description = "网点 ID") @PathVariable Long id,
                                       @Valid @RequestBody StatusDTO dto) {
        AdminStoreVO vo = adminStoreService.toggleStatus(id, dto.getStatus());
        return Result.success(Integer.valueOf(1).equals(dto.getStatus()) ? "网点已恢复营业" : "网点已停用", vo);
    }

    @Operation(summary = "删除网点", description = "只有零车辆、零订单的空网点可以删")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@Parameter(description = "网点 ID") @PathVariable Long id) {
        adminStoreService.delete(id);
        return Result.success("网点已删除", null);
    }
}
