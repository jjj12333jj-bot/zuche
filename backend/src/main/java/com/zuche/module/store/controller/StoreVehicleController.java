package com.zuche.module.store.controller;

import com.zuche.common.PageResult;
import com.zuche.common.Result;
import com.zuche.module.store.dto.MaintenanceCreateDTO;
import com.zuche.module.store.dto.MaintenanceQueryDTO;
import com.zuche.module.store.dto.StoreVehicleQueryDTO;
import com.zuche.module.store.dto.VehiclePriceDTO;
import com.zuche.module.store.dto.VehicleStatusDTO;
import com.zuche.module.store.service.StoreVehicleService;
import com.zuche.module.store.vo.MaintenanceVO;
import com.zuche.module.store.vo.StoreVehicleVO;
import com.zuche.module.system.annotation.OpLog;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 网点车辆与台账（仅网点管理员）
 *
 * <p>所有接口的 storeId 都从 JWT 里取，不接受前端传参——
 * 一旦允许传 storeId，等于把「看别家网点」做成了一个参数。</p>
 */
@Tag(name = "07-网点车辆", description = "本网点车辆、调价、上下架、保养维修台账")
@RestController
@RequestMapping("/api/store")
@RequiredArgsConstructor
@RequireRole(RoleCode.STORE_ADMIN)
@SecurityRequirement(name = "Authorization")
public class StoreVehicleController {

    private final StoreVehicleService storeVehicleService;

    @Operation(summary = "本网点车辆分页",
            description = "支持按状态筛选与车牌/编号/品牌车型搜索；返回台账摘要与当前占用订单")
    @GetMapping("/vehicles")
    public Result<PageResult<StoreVehicleVO>> vehicles(
            @ParameterObject @ModelAttribute StoreVehicleQueryDTO query) {
        return Result.success(storeVehicleService.vehicles(query));
    }

    @Operation(summary = "车辆调价", description = "同时改日租金与押金；已成交订单按快照价结算，不受影响")
    @PutMapping("/vehicles/{id}/price")
    @OpLog(module = "网点车辆", operation = "车辆调价")
    public Result<Void> updatePrice(@Parameter(description = "车辆 ID") @PathVariable Long id,
                                    @Valid @RequestBody VehiclePriceDTO dto) {
        return Result.success(storeVehicleService.updatePrice(id, dto), null);
    }

    @Operation(summary = "车辆上下架/进维保",
            description = "只接受 0 下架 / 1 可租 / 3 维保中；已租出由系统维护，有未结束订单时拒绝")
    @PutMapping("/vehicles/{id}/status")
    @OpLog(module = "网点车辆", operation = "上下架车辆")
    public Result<Void> updateStatus(@Parameter(description = "车辆 ID") @PathVariable Long id,
                                     @Valid @RequestBody VehicleStatusDTO dto) {
        return Result.success(storeVehicleService.updateStatus(id, dto), null);
    }

    @Operation(summary = "本网点台账分页",
            description = "支持按类型、车辆、关键词过滤；vehicleId 用于车辆详情抽屉")
    @GetMapping("/maintenances")
    public Result<PageResult<MaintenanceVO>> maintenances(
            @ParameterObject @ModelAttribute MaintenanceQueryDTO query) {
        return Result.success(storeVehicleService.maintenances(query));
    }

    @Operation(summary = "登记保养/维修台账",
            description = "里程大于档案值时自动更新车辆里程；登记维修且车辆空闲时自动置为维保中")
    @PostMapping("/vehicles/{id}/maintenances")
    @OpLog(module = "网点车辆", operation = "新增保养记录")
    public Result<Void> addMaintenance(@Parameter(description = "车辆 ID") @PathVariable Long id,
                                       @Valid @RequestBody MaintenanceCreateDTO dto) {
        return Result.success(storeVehicleService.addMaintenance(id, dto), null);
    }
}
