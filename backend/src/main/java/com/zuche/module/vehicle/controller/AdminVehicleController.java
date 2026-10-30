package com.zuche.module.vehicle.controller;

import com.zuche.common.PageResult;
import com.zuche.common.Result;
import com.zuche.common.StatusDTO;
import com.zuche.module.store.dto.MaintenanceCreateDTO;
import com.zuche.module.vehicle.dto.AdminMaintenanceQueryDTO;
import com.zuche.module.vehicle.dto.AdminVehicleQueryDTO;
import com.zuche.module.vehicle.dto.BrandQueryDTO;
import com.zuche.module.vehicle.dto.BrandSaveDTO;
import com.zuche.module.vehicle.dto.InsuranceQueryDTO;
import com.zuche.module.vehicle.dto.InsuranceSaveDTO;
import com.zuche.module.vehicle.dto.ModelQueryDTO;
import com.zuche.module.vehicle.dto.ModelSaveDTO;
import com.zuche.module.vehicle.dto.VehicleSaveDTO;
import com.zuche.module.vehicle.service.AdminVehicleService;
import com.zuche.module.vehicle.vo.AdminMaintenanceVO;
import com.zuche.module.vehicle.vo.AdminVehicleStatsVO;
import com.zuche.module.vehicle.vo.AdminVehicleVO;
import com.zuche.module.vehicle.vo.BrandManageVO;
import com.zuche.module.vehicle.vo.InsuranceVO;
import com.zuche.module.vehicle.vo.ModelManageVO;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 平台车辆资产（仅系统管理员）
 *
 * <p>覆盖 5 个子页面：品牌、车型、车辆档案、保养维修、保险记录。
 * 与网点侧的差别是这里<b>不按 storeId 过滤</b>——平台看全量，
 * 但所有写操作仍然受「未结束订单」约束，不能靠管理员身份绕过业务规则。</p>
 */
@Tag(name = "08-车辆资产", description = "品牌、车型、车辆档案、保养维修台账、保险单")
@RestController
@RequestMapping("/api/admin/vehicle")
@RequiredArgsConstructor
@RequireRole(RoleCode.SYS_ADMIN)
@SecurityRequirement(name = "Authorization")
public class AdminVehicleController {

    private final AdminVehicleService adminVehicleService;

    /* ==================== 品牌 ==================== */

    @Operation(summary = "资产统计", description = "品牌 / 台账 / 保单三组全量数字，不受分页影响")
    @GetMapping("/stats")
    public Result<AdminVehicleStatsVO> stats() {
        return Result.success(adminVehicleService.stats());
    }

    @Operation(summary = "品牌分页", description = "带车型数、车辆数、可租数；支持按名称/首字母搜索")
    @GetMapping("/brands")
    public Result<PageResult<BrandManageVO>> brands(@ParameterObject @ModelAttribute BrandQueryDTO query) {
        return Result.success(adminVehicleService.brands(query));
    }

    @Operation(summary = "启用品牌下拉", description = "车型表单用，只返回 status=1 的品牌")
    @GetMapping("/brands/options")
    public Result<List<BrandManageVO>> brandOptions() {
        return Result.success(adminVehicleService.brandOptions());
    }

    @Operation(summary = "新增品牌")
    @PostMapping("/brands")
    public Result<Void> createBrand(@Valid @RequestBody BrandSaveDTO dto) {
        return Result.success(adminVehicleService.saveBrand(null, dto), null);
    }

    @Operation(summary = "编辑品牌")
    @PutMapping("/brands/{id}")
    public Result<Void> updateBrand(@PathVariable Long id, @Valid @RequestBody BrandSaveDTO dto) {
        return Result.success(adminVehicleService.saveBrand(id, dto), null);
    }

    @Operation(summary = "启停品牌", description = "停用后前台不再展示该品牌车辆，在租订单不受影响")
    @PutMapping("/brands/{id}/status")
    public Result<Void> updateBrandStatus(@PathVariable Long id, @Valid @RequestBody StatusDTO dto) {
        return Result.success(adminVehicleService.updateBrandStatus(id, dto.getStatus()), null);
    }

    @Operation(summary = "删除品牌", description = "品牌下还有车型时拒绝删除")
    @DeleteMapping("/brands/{id}")
    public Result<Void> deleteBrand(@PathVariable Long id) {
        return Result.success(adminVehicleService.deleteBrand(id), null);
    }

    /* ==================== 车型 ==================== */

    @Operation(summary = "车型分页", description = "带品牌名、车辆数、在库最低日租金")
    @GetMapping("/models")
    public Result<PageResult<ModelManageVO>> models(@ParameterObject @ModelAttribute ModelQueryDTO query) {
        return Result.success(adminVehicleService.models(query));
    }

    @Operation(summary = "启用车型下拉", description = "车辆建档用，可按品牌过滤")
    @GetMapping("/models/options")
    public Result<List<ModelManageVO>> modelOptions(
            @Parameter(description = "品牌 ID，为空返回全部启用车型") Long brandId) {
        return Result.success(adminVehicleService.modelOptions(brandId));
    }

    @Operation(summary = "网点下拉", description = "车辆建档用，只返回启用网点")
    @GetMapping("/stores")
    public Result<List<StoreOptionVO>> storeOptions() {
        return Result.success(adminVehicleService.storeOptions());
    }

    @Operation(summary = "新增车型")
    @PostMapping("/models")
    public Result<Void> createModel(@Valid @RequestBody ModelSaveDTO dto) {
        return Result.success(adminVehicleService.saveModel(null, dto), null);
    }

    @Operation(summary = "编辑车型")
    @PutMapping("/models/{id}")
    public Result<Void> updateModel(@PathVariable Long id, @Valid @RequestBody ModelSaveDTO dto) {
        return Result.success(adminVehicleService.saveModel(id, dto), null);
    }

    @Operation(summary = "启停车型")
    @PutMapping("/models/{id}/status")
    public Result<Void> updateModelStatus(@PathVariable Long id, @Valid @RequestBody StatusDTO dto) {
        return Result.success(adminVehicleService.updateModelStatus(id, dto.getStatus()), null);
    }

    @Operation(summary = "删除车型", description = "车型下还有车辆时拒绝删除")
    @DeleteMapping("/models/{id}")
    public Result<Void> deleteModel(@PathVariable Long id) {
        return Result.success(adminVehicleService.deleteModel(id), null);
    }

    /* ==================== 车辆档案 ==================== */

    @Operation(summary = "车辆档案分页",
            description = "跨网点；带当前占用订单、保单到期日、下次保养日")
    @GetMapping("/list")
    public Result<PageResult<AdminVehicleVO>> vehicles(
            @ParameterObject @ModelAttribute AdminVehicleQueryDTO query) {
        return Result.success(adminVehicleService.vehicles(query));
    }

    @Operation(summary = "车辆建档")
    @PostMapping("/list")
    public Result<Void> createVehicle(@Valid @RequestBody VehicleSaveDTO dto) {
        return Result.success(adminVehicleService.saveVehicle(null, dto), null);
    }

    @Operation(summary = "编辑车辆档案",
            description = "有未结束订单时不允许改车型、归属网点与可用状态")
    @PutMapping("/list/{id}")
    public Result<Void> updateVehicle(@PathVariable Long id, @Valid @RequestBody VehicleSaveDTO dto) {
        return Result.success(adminVehicleService.saveVehicle(id, dto), null);
    }

    @Operation(summary = "车辆上下架/进维保", description = "只接受 0 下架 / 1 可租 / 3 维保中")
    @PutMapping("/list/{id}/status")
    public Result<Void> updateVehicleStatus(@PathVariable Long id, @Valid @RequestBody StatusDTO dto) {
        return Result.success(adminVehicleService.updateVehicleStatus(id, dto.getStatus()), null);
    }

    @Operation(summary = "删除车辆档案", description = "逻辑删除，历史订单仍可追溯")
    @DeleteMapping("/list/{id}")
    public Result<Void> deleteVehicle(@PathVariable Long id) {
        return Result.success(adminVehicleService.deleteVehicle(id), null);
    }

    /* ==================== 保养维修 ==================== */

    @Operation(summary = "保养维修台账分页", description = "带车辆、品牌车型与所属网点")
    @GetMapping("/maintenance")
    public Result<PageResult<AdminMaintenanceVO>> maintenances(
            @ParameterObject @ModelAttribute AdminMaintenanceQueryDTO query) {
        return Result.success(adminVehicleService.maintenances(query));
    }

    @Operation(summary = "平台登记台账",
            description = "里程大于档案值时自动更新里程；登记维修且车辆空闲时自动置为维保中")
    @PostMapping("/maintenance/vehicles/{vehicleId}")
    public Result<Void> addMaintenance(@PathVariable Long vehicleId,
                                       @Valid @RequestBody MaintenanceCreateDTO dto) {
        return Result.success(adminVehicleService.addMaintenance(vehicleId, dto), null);
    }

    /* ==================== 保险 ==================== */

    @Operation(summary = "保险记录分页",
            description = "带到期天数；expiringSoon=true 只看 30 天内到期")
    @GetMapping("/insurance")
    public Result<PageResult<InsuranceVO>> insurances(
            @ParameterObject @ModelAttribute InsuranceQueryDTO query) {
        return Result.success(adminVehicleService.insurances(query));
    }

    @Operation(summary = "登记保险单", description = "同车同险种时段不允许交叉")
    @PostMapping("/insurance")
    public Result<Void> createInsurance(@Valid @RequestBody InsuranceSaveDTO dto) {
        return Result.success(adminVehicleService.saveInsurance(null, dto), null);
    }

    @Operation(summary = "编辑保险单")
    @PutMapping("/insurance/{id}")
    public Result<Void> updateInsurance(@PathVariable Long id, @Valid @RequestBody InsuranceSaveDTO dto) {
        return Result.success(adminVehicleService.saveInsurance(id, dto), null);
    }

    @Operation(summary = "删除保险单")
    @DeleteMapping("/insurance/{id}")
    public Result<Void> deleteInsurance(@PathVariable Long id) {
        return Result.success(adminVehicleService.deleteInsurance(id), null);
    }
}
