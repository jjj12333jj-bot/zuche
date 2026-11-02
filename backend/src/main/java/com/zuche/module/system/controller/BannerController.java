package com.zuche.module.system.controller;

import com.zuche.common.Result;
import com.zuche.module.system.service.BannerService;
import com.zuche.module.system.vo.BannerVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 前台轮播图读取（无需登录）
 *
 * <p>只返回「显示中」的记录，按排序号升序；后台改完排序或显隐，前台刷新即生效。</p>
 */
@Tag(name = "01-首页轮播", description = "前台读取显示中的轮播图")
@RestController
@RequestMapping("/api/banners")
@RequiredArgsConstructor
public class BannerController {

    private final BannerService bannerService;

    @Operation(summary = "显示中的轮播图", description = "按排序号升序；不传 position 返回全部位置")
    @GetMapping
    public Result<List<BannerVO>> list(
            @Parameter(description = "位置：HOME 首页 / PORTAL 列表页，缺省返回全部")
            @RequestParam(required = false) String position) {
        return Result.success(bannerService.visible(position));
    }
}
