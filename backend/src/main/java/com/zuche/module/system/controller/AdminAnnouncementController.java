package com.zuche.module.system.controller;

import com.zuche.common.PageResult;
import com.zuche.common.Result;
import com.zuche.common.StatusDTO;
import com.zuche.module.system.annotation.OpLog;
import com.zuche.module.system.dto.AnnouncementQueryDTO;
import com.zuche.module.system.dto.AnnouncementSaveDTO;
import com.zuche.module.system.service.AnnouncementService;
import com.zuche.module.system.vo.AnnouncementStatsVO;
import com.zuche.module.system.vo.AnnouncementVO;
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
 * 平台公告管理（仅系统管理员）
 */
@Tag(name = "14-公告管理", description = "公告查询、新增、编辑、发布/下架、删除")
@RestController
@RequestMapping("/api/admin/announcements")
@RequiredArgsConstructor
@RequireRole(RoleCode.SYS_ADMIN)
@SecurityRequirement(name = "Authorization")
public class AdminAnnouncementController {

    private final AnnouncementService announcementService;

    @Operation(summary = "公告统计", description = "总数 / 已发布 / 草稿 / 累计浏览量")
    @GetMapping("/stats")
    public Result<AnnouncementStatsVO> stats() {
        return Result.success(announcementService.stats());
    }

    @Operation(summary = "公告分页", description = "列表不返回正文，正文在详情接口取")
    @GetMapping
    public Result<PageResult<AnnouncementVO>> page(@ParameterObject @ModelAttribute AnnouncementQueryDTO query) {
        return Result.success(announcementService.page(query));
    }

    @Operation(summary = "公告详情", description = "含正文，后台查看不计浏览量")
    @GetMapping("/{id}")
    public Result<AnnouncementVO> detail(@Parameter(description = "公告 ID") @PathVariable Long id) {
        return Result.success(announcementService.detail(id));
    }

    @Operation(summary = "新增公告", description = "status 不传按草稿保存")
    @PostMapping
    @OpLog(module = "公告管理", operation = "新增公告")
    public Result<AnnouncementVO> create(@Valid @RequestBody AnnouncementSaveDTO dto) {
        return Result.success("公告已保存", announcementService.create(dto));
    }

    @Operation(summary = "编辑公告", description = "已发布公告的发布时间不会被编辑覆盖")
    @PutMapping("/{id}")
    @OpLog(module = "公告管理", operation = "编辑公告")
    public Result<AnnouncementVO> update(@Parameter(description = "公告 ID") @PathVariable Long id,
                                         @Valid @RequestBody AnnouncementSaveDTO dto) {
        return Result.success("公告已更新", announcementService.update(id, dto));
    }

    @Operation(summary = "发布/下架", description = "发布时写入发布时间，重新发布即刷新为当前时间")
    @PutMapping("/{id}/status")
    @OpLog(module = "公告管理", operation = "发布/下架公告")
    public Result<AnnouncementVO> status(@Parameter(description = "公告 ID") @PathVariable Long id,
                                         @Valid @RequestBody StatusDTO dto) {
        AnnouncementVO vo = announcementService.toggleStatus(id, dto.getStatus());
        return Result.success(Integer.valueOf(1).equals(dto.getStatus()) ? "公告已发布" : "公告已下架", vo);
    }

    @Operation(summary = "删除公告", description = "逻辑删除，历史浏览量仍保留在库中")
    @DeleteMapping("/{id}")
    @OpLog(module = "公告管理", operation = "删除公告")
    public Result<Void> delete(@Parameter(description = "公告 ID") @PathVariable Long id) {
        announcementService.delete(id);
        return Result.success("公告已删除", null);
    }
}
