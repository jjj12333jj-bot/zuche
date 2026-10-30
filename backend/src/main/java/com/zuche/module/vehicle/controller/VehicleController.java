package com.zuche.module.vehicle.controller;

import com.zuche.common.PageResult;
import com.zuche.common.Result;
import com.zuche.module.vehicle.dto.VehicleQueryDTO;
import com.zuche.module.vehicle.service.VehicleService;
import com.zuche.module.vehicle.vo.VehicleDetailVO;
import com.zuche.module.vehicle.vo.VehicleListVO;
import com.zuche.module.vehicle.vo.VehicleOptionsVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 车辆浏览接口（公开）
 *
 * <p>列表默认过滤下架车辆；已租出/维修中仍展示，由前端用状态标签区分，
 * 避免用户搜索时“车突然消失”。</p>
 */
@Tag(name = "02-车辆", description = "车辆列表、筛选、详情")
@RestController
@RequestMapping("/api/vehicles")
@RequiredArgsConstructor
public class VehicleController {

    private final VehicleService vehicleService;

    @Operation(summary = "车辆分页列表", description = "支持关键词、分类、品牌、网点、价格区间、排序")
    @GetMapping
    public Result<PageResult<VehicleListVO>> page(@ParameterObject @ModelAttribute VehicleQueryDTO query) {
        return Result.success(vehicleService.page(query));
    }

    @Operation(summary = "筛选项", description = "分类（字典）、品牌、网点、价格区间")
    @GetMapping("/options")
    public Result<VehicleOptionsVO> options() {
        return Result.success(vehicleService.options());
    }

    @Operation(summary = "车辆详情", description = "含图集、门店信息、车型参数；每次访问累加浏览量")
    @GetMapping("/{id}")
    public Result<VehicleDetailVO> detail(@PathVariable Long id) {
        return Result.success(vehicleService.detail(id));
    }
}
