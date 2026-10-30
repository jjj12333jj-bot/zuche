package com.zuche.module.system.controller;

import com.zuche.common.PageResult;
import com.zuche.common.Result;
import com.zuche.common.StatusDTO;
import com.zuche.module.system.annotation.OpLog;
import com.zuche.module.system.dto.BannerQueryDTO;
import com.zuche.module.system.dto.BannerSaveDTO;
import com.zuche.module.system.service.BannerService;
import com.zuche.module.system.vo.BannerStatsVO;
import com.zuche.module.system.vo.BannerVO;
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
 * 首页轮播图管理（仅系统管理员）
 */
@Tag(name = "15-轮播图管理", description = "轮播图查询、新增、编辑、排序、显隐、删除")
@RestController
@RequestMapping("/api/admin/banners")
@RequiredArgsConstructor
@RequireRole(RoleCode.SYS_ADMIN)
@SecurityRequirement(name = "Authorization")
public class AdminBannerController {

    private final BannerService bannerService;

    @Operation(summary = "轮播图统计", description = "总数 / 显示中 / 已隐藏")
    @GetMapping("/stats")
    public Result<BannerStatsVO> stats() {
        return Result.success(bannerService.stats());
    }

    @Operation(summary = "轮播图分页", description = "显示中在前，同状态按排序号升序")
    @GetMapping
    public Result<PageResult<BannerVO>> page(@ParameterObject @ModelAttribute BannerQueryDTO query) {
        return Result.success(bannerService.page(query));
    }

    @Operation(summary = "轮播图详情")
    @GetMapping("/{id}")
    public Result<BannerVO> detail(@Parameter(description = "轮播图 ID") @PathVariable Long id) {
        return Result.success(bannerService.detail(id));
    }

    @Operation(summary = "新增轮播图", description = "不填排序号则自动排到该位置末尾")
    @PostMapping
    @OpLog(module = "轮播图管理", operation = "新增轮播图")
    public Result<BannerVO> create(@Valid @RequestBody BannerSaveDTO dto) {
        return Result.success("轮播图已创建", bannerService.create(dto));
    }

    @Operation(summary = "编辑轮播图")
    @PutMapping("/{id}")
    @OpLog(module = "轮播图管理", operation = "编辑轮播图")
    public Result<BannerVO> update(@Parameter(description = "轮播图 ID") @PathVariable Long id,
                                   @Valid @RequestBody BannerSaveDTO dto) {
        return Result.success("轮播图已更新", bannerService.update(id, dto));
    }

    @Operation(summary = "显示/隐藏", description = "活动结束后先隐藏，不必删除")
    @PutMapping("/{id}/status")
    @OpLog(module = "轮播图管理", operation = "显示/隐藏轮播图")
    public Result<BannerVO> status(@Parameter(description = "轮播图 ID") @PathVariable Long id,
                                   @Valid @RequestBody StatusDTO dto) {
        BannerVO vo = bannerService.toggleStatus(id, dto.getStatus());
        return Result.success(Integer.valueOf(1).equals(dto.getStatus()) ? "轮播图已显示" : "轮播图已隐藏", vo);
    }

    @Operation(summary = "删除轮播图")
    @DeleteMapping("/{id}")
    @OpLog(module = "轮播图管理", operation = "删除轮播图")
    public Result<Void> delete(@Parameter(description = "轮播图 ID") @PathVariable Long id) {
        bannerService.delete(id);
        return Result.success("轮播图已删除", null);
    }
}
