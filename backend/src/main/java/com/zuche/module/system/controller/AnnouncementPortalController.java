package com.zuche.module.system.controller;

import com.zuche.common.Result;
import com.zuche.module.system.service.AnnouncementService;
import com.zuche.module.system.vo.AnnouncementVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 前台公告（免登录）
 *
 * <p>路径在 WebMvcConfig 的白名单里，未登录用户也能看公告——
 * 「押金退还时效说明」这类内容本来就该让还没注册的人看到。</p>
 */
@Tag(name = "17-前台公告", description = "已发布公告列表与详情，免登录")
@RestController
@RequestMapping("/api/announcements")
@RequiredArgsConstructor
public class AnnouncementPortalController {

    private final AnnouncementService announcementService;

    @Operation(summary = "公告列表", description = "仅已发布，按发布时间倒序，默认 5 条")
    @GetMapping
    public Result<List<AnnouncementVO>> list(
            @Parameter(description = "条数，默认 5，最多 20") @RequestParam(required = false) Integer limit) {
        return Result.success(announcementService.published(limit));
    }

    @Operation(summary = "公告详情", description = "每次访问浏览量 +1")
    @GetMapping("/{id}")
    public Result<AnnouncementVO> detail(@Parameter(description = "公告 ID") @PathVariable Long id) {
        return Result.success(announcementService.publicDetail(id));
    }
}
