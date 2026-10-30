package com.zuche.module.vehicle.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zuche.common.BusinessException;
import com.zuche.common.PageResult;
import com.zuche.common.ResultCode;
import com.zuche.module.store.dto.MaintenanceCreateDTO;
import com.zuche.module.store.entity.Store;
import com.zuche.module.store.entity.VehicleMaintenance;
import com.zuche.module.store.mapper.StoreMapper;
import com.zuche.module.store.mapper.VehicleMaintenanceMapper;
import com.zuche.module.vehicle.dto.AdminMaintenanceQueryDTO;
import com.zuche.module.vehicle.dto.AdminVehicleQueryDTO;
import com.zuche.module.vehicle.dto.BrandQueryDTO;
import com.zuche.module.vehicle.dto.BrandSaveDTO;
import com.zuche.module.vehicle.dto.InsuranceQueryDTO;
import com.zuche.module.vehicle.dto.InsuranceSaveDTO;
import com.zuche.module.vehicle.dto.ModelQueryDTO;
import com.zuche.module.vehicle.dto.ModelSaveDTO;
import com.zuche.module.vehicle.dto.VehicleSaveDTO;
import com.zuche.module.vehicle.entity.Vehicle;
import com.zuche.module.vehicle.entity.VehicleBrand;
import com.zuche.module.vehicle.entity.VehicleInsurance;
import com.zuche.module.vehicle.entity.VehicleModel;
import com.zuche.module.vehicle.mapper.AdminVehicleMapper;
import com.zuche.module.vehicle.mapper.VehicleBrandMapper;
import com.zuche.module.vehicle.mapper.VehicleInsuranceMapper;
import com.zuche.module.vehicle.mapper.VehicleMapper;
import com.zuche.module.vehicle.mapper.VehicleModelMapper;
import com.zuche.module.vehicle.vo.AdminMaintenanceVO;
import com.zuche.module.vehicle.vo.AdminVehicleStatsVO;
import com.zuche.module.vehicle.vo.AdminVehicleVO;
import com.zuche.module.vehicle.vo.BrandManageVO;
import com.zuche.module.vehicle.vo.InsuranceVO;
import com.zuche.module.vehicle.vo.ModelManageVO;
import com.zuche.module.vehicle.vo.StoreOptionVO;
import com.zuche.security.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 平台车辆资产服务：品牌 / 车型 / 车辆档案 / 保养台账 / 保险单
 *
 * <p>贯穿本模块的三条硬规则：</p>
 * <ol>
 *   <li><b>停用可以，删除要拦。</b>品牌下还有车型、车型下还有车，就不允许物理删除；
 *       但允许停用——停用只是不再对外展示，历史订单仍然查得到。</li>
 *   <li><b>有未结束订单的车，不能改归属、不能改车型、不能下架。</b>
 *       订单是按「网点 + 车型快照」结账的，中途改掉会让账单和资产对不上。</li>
 *   <li><b>车辆的品牌永远由车型反查。</b>不允许单独填 brandId，
 *       否则会出现「车型是朗逸、品牌填丰田」的脏数据。</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
public class AdminVehicleService {

    private static final Map<Integer, String> VEHICLE_STATUS_LABELS = Map.of(
            0, "下架", 1, "可租", 2, "已租出", 3, "维保中");

    private static final Map<String, String> CATEGORY_LABELS = Map.of(
            "ECONOMY", "经济型", "COMFORT", "舒适型", "SUV", "SUV",
            "LUXURY", "豪华型", "EV", "新能源");

    private static final Map<String, String> GEARBOX_LABELS = Map.of(
            "AUTO", "自动挡", "MANUAL", "手动挡");

    private static final Map<String, String> FUEL_LABELS = Map.of(
            "GASOLINE", "汽油", "HYBRID", "混动", "ELECTRIC", "纯电");

    private static final Map<String, String> MAINT_LABELS = Map.of(
            "MAINTAIN", "保养", "REPAIR", "维修");

    private static final Map<String, String> INSURANCE_LABELS = Map.of(
            "COMPULSORY", "交强险", "COMMERCIAL", "商业险", "PASSENGER", "座位险");

    private final AdminVehicleMapper adminVehicleMapper;
    private final VehicleBrandMapper brandMapper;
    private final VehicleModelMapper modelMapper;
    private final VehicleMapper vehicleMapper;
    private final VehicleInsuranceMapper insuranceMapper;
    private final VehicleMaintenanceMapper maintenanceMapper;
    private final StoreMapper storeMapper;

    /* ==================================================================
       品牌
       ================================================================== */

    public PageResult<BrandManageVO> brands(BrandQueryDTO q) {
        IPage<BrandManageVO> page = adminVehicleMapper.selectBrandPage(
                new Page<>(q.getCurrent(), q.getPageSize()), q);
        return PageResult.of(page);
    }

    public List<BrandManageVO> brandOptions() {
        return adminVehicleMapper.selectBrandOptions();
    }

    @Transactional(rollbackFor = Exception.class)
    public String saveBrand(Long id, BrandSaveDTO dto) {
        String name = dto.getBrandName().trim();

        LambdaQueryWrapper<VehicleBrand> dup = new LambdaQueryWrapper<VehicleBrand>()
                .eq(VehicleBrand::getBrandName, name);
        if (id != null) {
            dup.ne(VehicleBrand::getId, id);
        }
        if (brandMapper.selectCount(dup) > 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "品牌「" + name + "」已存在");
        }

        VehicleBrand brand = new VehicleBrand();
        brand.setBrandName(name);
        brand.setBrandLogo(trimToNull(dto.getBrandLogo()));
        brand.setInitial(resolveInitial(dto.getInitial(), name));
        brand.setSortNo(dto.getSortNo() == null ? 99 : dto.getSortNo());
        brand.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());

        if (id == null) {
            brandMapper.insert(brand);
            return "品牌「" + name + "」已新增";
        }

        requireBrand(id);
        brand.setId(id);
        brandMapper.updateById(brand);
        return "品牌「" + name + "」已更新";
    }

    /**
     * 启停品牌
     *
     * <p>停用不做强制拦截，但把影响面说清楚：品牌停用后前台不再展示，
     * 已在租的订单不受影响（它们用的是车型快照）。</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public String updateBrandStatus(Long id, Integer status) {
        VehicleBrand brand = requireBrand(id);
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "状态只能是 0 停用 / 1 启用");
        }

        VehicleBrand update = new VehicleBrand();
        update.setId(id);
        update.setStatus(status);
        brandMapper.updateById(update);

        if (status == 0) {
            int vehicles = adminVehicleMapper.countVehiclesOfBrand(id);
            return "品牌「" + brand.getBrandName() + "」已停用"
                    + (vehicles > 0 ? "，该品牌下 " + vehicles + " 台车将不再出现在前台车辆列表" : "");
        }
        return "品牌「" + brand.getBrandName() + "」已启用";
    }

    @Transactional(rollbackFor = Exception.class)
    public String deleteBrand(Long id) {
        VehicleBrand brand = requireBrand(id);

        int models = adminVehicleMapper.countModelsOfBrand(id);
        if (models > 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST,
                    "该品牌下还有 " + models + " 个车型，请先停用或删除车型");
        }
        brandMapper.deleteById(id);
        return "品牌「" + brand.getBrandName() + "」已删除";
    }

    /* ==================================================================
       车型
       ================================================================== */

    public PageResult<ModelManageVO> models(ModelQueryDTO q) {
        IPage<ModelManageVO> page = adminVehicleMapper.selectModelPage(
                new Page<>(q.getCurrent(), q.getPageSize()), q);
        page.getRecords().forEach(this::decorateModel);
        return PageResult.of(page);
    }

    public List<ModelManageVO> modelOptions(Long brandId) {
        List<ModelManageVO> list = adminVehicleMapper.selectModelOptions(brandId);
        list.forEach(this::decorateModel);
        return list;
    }

    /**
     * 网点下拉
     *
     * <p>车辆建档必须选网点。这里只返回启用网点，避免把车挂到一个已停用的网点上——
     * 停用网点下的车在前台是查不到的，挂过去等于「凭空消失」。</p>
     */
    public List<StoreOptionVO> storeOptions() {
        List<Store> stores = storeMapper.selectList(new LambdaQueryWrapper<Store>()
                .eq(Store::getStatus, 1)
                .orderByAsc(Store::getId));
        return stores.stream().map(s -> {
            StoreOptionVO vo = new StoreOptionVO();
            vo.setId(s.getId());
            vo.setName(s.getStoreName());
            vo.setAddress(s.getAddress());
            return vo;
        }).toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public String saveModel(Long id, ModelSaveDTO dto) {
        requireBrand(dto.getBrandId());
        String name = dto.getModelName().trim();

        LambdaQueryWrapper<VehicleModel> dup = new LambdaQueryWrapper<VehicleModel>()
                .eq(VehicleModel::getBrandId, dto.getBrandId())
                .eq(VehicleModel::getModelName, name);
        if (id != null) {
            dup.ne(VehicleModel::getId, id);
        }
        if (modelMapper.selectCount(dup) > 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "该品牌下已存在车型「" + name + "」");
        }

        VehicleModel model = new VehicleModel();
        model.setBrandId(dto.getBrandId());
        model.setModelName(name);
        model.setCategoryCode(dto.getCategoryCode());
        model.setSeats(dto.getSeats());
        model.setGearbox(dto.getGearbox());
        model.setFuelType(dto.getFuelType());
        model.setDisplacement(trimToNull(dto.getDisplacement()));
        model.setGuidePrice(dto.getGuidePrice());
        model.setDeposit(dto.getDeposit());
        model.setCoverImage(trimToNull(dto.getCoverImage()));
        model.setDescription(trimToNull(dto.getDescription()));
        model.setSortNo(dto.getSortNo() == null ? 99 : dto.getSortNo());
        model.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());

        if (id == null) {
            modelMapper.insert(model);
            return "车型「" + name + "」已新增";
        }

        requireModel(id);
        model.setId(id);
        modelMapper.updateById(model);
        return "车型「" + name + "」已更新";
    }

    @Transactional(rollbackFor = Exception.class)
    public String updateModelStatus(Long id, Integer status) {
        VehicleModel model = requireModel(id);
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "状态只能是 0 停用 / 1 启用");
        }

        VehicleModel update = new VehicleModel();
        update.setId(id);
        update.setStatus(status);
        modelMapper.updateById(update);

        int vehicles = adminVehicleMapper.countVehiclesOfModel(id);
        if (status == 0) {
            return "车型「" + model.getModelName() + "」已停用"
                    + (vehicles > 0 ? "，该车型下 " + vehicles + " 台车将不再出现在前台" : "");
        }
        return "车型「" + model.getModelName() + "」已启用";
    }

    @Transactional(rollbackFor = Exception.class)
    public String deleteModel(Long id) {
        VehicleModel model = requireModel(id);

        int vehicles = adminVehicleMapper.countVehiclesOfModel(id);
        if (vehicles > 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST,
                    "该车型下还有 " + vehicles + " 台车，请先停用或删除车辆档案");
        }
        modelMapper.deleteById(id);
        return "车型「" + model.getModelName() + "」已删除";
    }

    /* ==================================================================
       车辆档案
       ================================================================== */

    public PageResult<AdminVehicleVO> vehicles(AdminVehicleQueryDTO q) {
        IPage<AdminVehicleVO> page = adminVehicleMapper.selectVehiclePage(
                new Page<>(q.getCurrent(), q.getPageSize()), q);
        page.getRecords().forEach(this::decorateVehicle);
        return PageResult.of(page);
    }

    @Transactional(rollbackFor = Exception.class)
    public String saveVehicle(Long id, VehicleSaveDTO dto) {
        VehicleModel model = modelMapper.selectById(dto.getModelId());
        if (model == null || Integer.valueOf(1).equals(model.getDeleted())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "所选车型不存在");
        }

        String vehicleNo = dto.getVehicleNo().trim();
        String plate = dto.getLicensePlate().trim().toUpperCase();

        LambdaQueryWrapper<Vehicle> dupNo = new LambdaQueryWrapper<Vehicle>()
                .eq(Vehicle::getVehicleNo, vehicleNo);
        LambdaQueryWrapper<Vehicle> dupPlate = new LambdaQueryWrapper<Vehicle>()
                .eq(Vehicle::getLicensePlate, plate);
        if (id != null) {
            dupNo.ne(Vehicle::getId, id);
            dupPlate.ne(Vehicle::getId, id);
        }
        if (vehicleMapper.selectCount(dupNo) > 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "车辆编号「" + vehicleNo + "」已存在");
        }
        if (vehicleMapper.selectCount(dupPlate) > 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "车牌「" + plate + "」已登记过");
        }

        Integer targetStatus = dto.getStatus() == null ? 1 : dto.getStatus();
        if (targetStatus == 2) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "「已租出」由系统在支付时维护，不能手工设置");
        }

        if (id == null) {
            Vehicle vehicle = new Vehicle();
            applyVehicleFields(vehicle, dto, model, vehicleNo, plate, targetStatus);
            vehicle.setRentCount(0);
            vehicle.setViewCount(0);
            vehicleMapper.insert(vehicle);
            return "车辆「" + plate + "」已建档";
        }

        Vehicle existing = requireVehicle(id);
        // 有未结束订单时，改动归属或车型会让订单结账时的网点/车型快照对不上
        int active = adminVehicleMapper.countActiveOrdersOfVehicle(id);
        if (active > 0) {
            boolean moved = !dto.getModelId().equals(existing.getModelId())
                    || !dto.getStoreId().equals(existing.getStoreId());
            if (moved) {
                throw new BusinessException(ResultCode.BAD_REQUEST,
                        "该车存在未结束订单，不能修改车型或归属网点；请等订单结束后再调整");
            }
            if (!targetStatus.equals(existing.getStatus())) {
                throw new BusinessException(ResultCode.BAD_REQUEST,
                        "该车存在未结束订单，不能修改可用状态；请先在订单管理中处理订单");
            }
        }

        Vehicle update = new Vehicle();
        update.setId(id);
        applyVehicleFields(update, dto, model, vehicleNo, plate, targetStatus);
        vehicleMapper.updateById(update);
        return "车辆「" + plate + "」档案已更新";
    }

    /**
     * 单独改可用状态
     *
     * <p>只接受 0 下架 / 1 可租 / 3 维保中。置为可租前再查一次未结束订单——
     * 前端可能停留在旧数据上，人点下去的时候订单刚成立。</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public String updateVehicleStatus(Long id, Integer status) {
        Vehicle vehicle = requireVehicle(id);
        if (status == null || (status != 0 && status != 1 && status != 3)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "状态只能是 0 下架 / 1 可租 / 3 维保中");
        }
        if (Integer.valueOf(2).equals(vehicle.getStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST,
                    "该车当前已租出，需等订单归还后才能调整状态");
        }
        int active = adminVehicleMapper.countActiveOrdersOfVehicle(id);
        if (active > 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST,
                    "该车存在未结束订单，不能调整可用状态");
        }

        Vehicle update = new Vehicle();
        update.setId(id);
        update.setStatus(status);
        vehicleMapper.updateById(update);

        String tail = switch (status) {
            case 0 -> "，已下架不再对外展示";
            case 1 -> "，已重新对外可租";
            default -> "，已置为维保中暂不可租";
        };
        return "车辆「" + vehicle.getLicensePlate() + "」" + tail;
    }

    @Transactional(rollbackFor = Exception.class)
    public String deleteVehicle(Long id) {
        Vehicle vehicle = requireVehicle(id);
        int active = adminVehicleMapper.countActiveOrdersOfVehicle(id);
        if (active > 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST,
                    "该车存在未结束订单，不能删除档案");
        }
        // 逻辑删除：历史订单的 order_item 仍指向这台车，物理删除会让订单详情查不到车
        vehicleMapper.deleteById(id);
        return "车辆「" + vehicle.getLicensePlate() + "」档案已删除（历史订单仍可追溯）";
    }

    /* ==================================================================
       保养维修台账
       ================================================================== */

    public PageResult<AdminMaintenanceVO> maintenances(AdminMaintenanceQueryDTO q) {
        IPage<AdminMaintenanceVO> page = adminVehicleMapper.selectMaintenancePage(
                new Page<>(q.getCurrent(), q.getPageSize()), q);
        page.getRecords().forEach(vo ->
                vo.setMaintLabel(label(MAINT_LABELS, vo.getMaintType())));
        return PageResult.of(page);
    }

    /**
     * 平台登记台账
     *
     * <p>与网点登记同一套连带规则：里程往前推、维修自动置维保中。
     * 差别只是平台可以给任意网点的车登记。</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public String addMaintenance(Long vehicleId, MaintenanceCreateDTO dto) {
        Vehicle vehicle = requireVehicle(vehicleId);

        if (dto.getNextDate() != null && !dto.getNextDate().isAfter(dto.getMaintDate())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "下次保养日期必须晚于本次进场日期");
        }

        VehicleMaintenance record = new VehicleMaintenance();
        record.setVehicleId(vehicleId);
        record.setMaintType(dto.getMaintType());
        record.setMaintDate(dto.getMaintDate());
        record.setMileage(dto.getMileage());
        record.setCost(dto.getCost() == null ? BigDecimal.ZERO : dto.getCost());
        record.setContent(dto.getContent().trim());
        record.setNextDate(dto.getNextDate());
        record.setRemark(trimToNull(dto.getRemark()));
        record.setOperatorId(UserContext.requireUserId());
        maintenanceMapper.insert(record);

        StringBuilder message = new StringBuilder("台账已登记（")
                .append(MAINT_LABELS.getOrDefault(dto.getMaintType(), dto.getMaintType()))
                .append(" · ").append(dto.getMaintDate()).append("）");

        if (dto.getMileage() != null
                && (vehicle.getCurrentMileage() == null
                    || dto.getMileage().compareTo(vehicle.getCurrentMileage()) > 0)) {
            Vehicle mileageUpdate = new Vehicle();
            mileageUpdate.setId(vehicleId);
            mileageUpdate.setCurrentMileage(dto.getMileage());
            vehicleMapper.updateById(mileageUpdate);
            message.append("，车辆里程已更新为 ").append(dto.getMileage()).append(" 万公里");
        }

        if ("REPAIR".equals(dto.getMaintType())
                && Integer.valueOf(1).equals(vehicle.getStatus())
                && adminVehicleMapper.countActiveOrdersOfVehicle(vehicleId) == 0) {
            Vehicle statusUpdate = new Vehicle();
            statusUpdate.setId(vehicleId);
            statusUpdate.setStatus(3);
            vehicleMapper.updateById(statusUpdate);
            message.append("，车辆已自动置为维保中，暂不可租");
        }

        return message.append("。").toString();
    }

    /* ==================================================================
       保险单
       ================================================================== */

    public PageResult<InsuranceVO> insurances(InsuranceQueryDTO q) {
        IPage<InsuranceVO> page = adminVehicleMapper.selectInsurancePage(
                new Page<>(q.getCurrent(), q.getPageSize()), q);
        page.getRecords().forEach(this::decorateInsurance);
        return PageResult.of(page);
    }

    @Transactional(rollbackFor = Exception.class)
    public String saveInsurance(Long id, InsuranceSaveDTO dto) {
        Vehicle vehicle = requireVehicle(dto.getVehicleId());

        if (!dto.getEndDate().isAfter(dto.getStartDate())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "到期日期必须晚于生效日期");
        }

        // 同车同险种时段不允许交叉；首尾相接（旧单 1/1 到期、新单 1/1 生效）是允许的
        LambdaQueryWrapper<VehicleInsurance> overlap = new LambdaQueryWrapper<VehicleInsurance>()
                .eq(VehicleInsurance::getVehicleId, dto.getVehicleId())
                .eq(VehicleInsurance::getInsuranceType, dto.getInsuranceType())
                .lt(VehicleInsurance::getStartDate, dto.getEndDate())
                .gt(VehicleInsurance::getEndDate, dto.getStartDate());
        if (id != null) {
            overlap.ne(VehicleInsurance::getId, id);
        }
        if (insuranceMapper.selectCount(overlap) > 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST,
                    "该车同险种已存在时段重叠的保单，请先核对起止日期");
        }

        VehicleInsurance insurance = new VehicleInsurance();
        insurance.setVehicleId(dto.getVehicleId());
        insurance.setInsuranceType(dto.getInsuranceType());
        insurance.setInsurer(dto.getInsurer().trim());
        insurance.setPolicyNo(dto.getPolicyNo().trim());
        insurance.setStartDate(dto.getStartDate());
        insurance.setEndDate(dto.getEndDate());
        insurance.setPremium(dto.getPremium());
        // 生效中只看「今天是否落在保单区间内」，避免手工维护状态与日期打架
        LocalDate today = LocalDate.now();
        insurance.setStatus(
                today.isBefore(dto.getEndDate()) && !today.isBefore(dto.getStartDate()) ? 1 : 0);

        String typeLabel = INSURANCE_LABELS.getOrDefault(dto.getInsuranceType(), dto.getInsuranceType());

        if (id == null) {
            insuranceMapper.insert(insurance);
            return "保单已登记（" + vehicle.getLicensePlate() + " · " + typeLabel + "）";
        }

        requireInsurance(id);
        insurance.setId(id);
        insuranceMapper.updateById(insurance);
        return "保单已更新（" + vehicle.getLicensePlate() + " · " + typeLabel + "）";
    }

    @Transactional(rollbackFor = Exception.class)
    public String deleteInsurance(Long id) {
        VehicleInsurance insurance = requireInsurance(id);
        insuranceMapper.deleteById(id);
        return "保单「" + insurance.getPolicyNo() + "」已删除";
    }

    /* ==================================================================
       统计
       ================================================================== */

    /** 三个子页面的顶部数字卡，一次请求拿全，全部走库内聚合 */
    public AdminVehicleStatsVO stats() {
        AdminVehicleStatsVO vo = new AdminVehicleStatsVO();
        vo.setBrand(adminVehicleMapper.selectBrandStats());
        vo.setMaintenance(adminVehicleMapper.selectMaintenanceStats());
        vo.setInsurance(adminVehicleMapper.selectInsuranceStats());
        return vo;
    }

    /* ==================================================================
       内部
       ================================================================== */

    private void applyVehicleFields(Vehicle vehicle, VehicleSaveDTO dto, VehicleModel model,
                                    String vehicleNo, String plate, Integer status) {
        vehicle.setVehicleNo(vehicleNo);
        vehicle.setModelId(model.getId());
        // 品牌永远由车型反查，不接受前端传
        vehicle.setBrandId(model.getBrandId());
        vehicle.setStoreId(dto.getStoreId());
        vehicle.setLicensePlate(plate);
        vehicle.setVin(trimToNull(dto.getVin()));
        vehicle.setColor(trimToNull(dto.getColor()));
        vehicle.setCurrentMileage(dto.getCurrentMileage() == null ? BigDecimal.ZERO : dto.getCurrentMileage());
        vehicle.setDailyPrice(dto.getDailyPrice() == null ? model.getGuidePrice() : dto.getDailyPrice());
        vehicle.setDeposit(dto.getDeposit() == null ? model.getDeposit() : dto.getDeposit());
        vehicle.setDiscountRate(dto.getDiscountRate() == null ? BigDecimal.ONE : dto.getDiscountRate());
        vehicle.setRegisterDate(dto.getRegisterDate());
        vehicle.setStatus(status);
        vehicle.setDescription(trimToNull(dto.getDescription()));
    }

    private void decorateVehicle(AdminVehicleVO vo) {
        vo.setStatusLabel(label(VEHICLE_STATUS_LABELS, vo.getStatus()));
    }

    /**
     * 空安全取标签
     *
     * <p>不能用 {@code map.getOrDefault(code, code)}：Map.of 创建的不可变 Map
     * 在 key 为 null 时会直接抛 NPE（源码里是 pk.hashCode()），
     * 而「下拉选项只查了部分字段」就会命中这种情况。</p>
     */
    private static String label(Map<?, String> dict, Object code) {
        if (code == null) {
            return null;
        }
        return dict.getOrDefault(code, String.valueOf(code));
    }

    private void decorateModel(ModelManageVO vo) {
        vo.setCategoryLabel(label(CATEGORY_LABELS, vo.getCategoryCode()));
        vo.setGearboxLabel(label(GEARBOX_LABELS, vo.getGearbox()));
        vo.setFuelLabel(label(FUEL_LABELS, vo.getFuelType()));
    }

    private void decorateInsurance(InsuranceVO vo) {
        vo.setTypeLabel(label(INSURANCE_LABELS, vo.getInsuranceType()));
        Integer days = vo.getDaysToExpire();
        if (vo.getStatus() != null && vo.getStatus() == 0) {
            vo.setStatusLabel("已失效");
        } else if (days == null) {
            vo.setStatusLabel("生效中");
        } else if (days < 0) {
            vo.setStatusLabel("已过期");
        } else if (days <= 30) {
            vo.setStatusLabel(days + " 天后到期");
        } else {
            vo.setStatusLabel("生效中");
        }
    }

    private VehicleBrand requireBrand(Long id) {
        VehicleBrand brand = brandMapper.selectById(id);
        if (brand == null || Integer.valueOf(1).equals(brand.getDeleted())) {
            throw new BusinessException(ResultCode.NOT_FOUND, "品牌不存在");
        }
        return brand;
    }

    private VehicleModel requireModel(Long id) {
        VehicleModel model = modelMapper.selectById(id);
        if (model == null || Integer.valueOf(1).equals(model.getDeleted())) {
            throw new BusinessException(ResultCode.NOT_FOUND, "车型不存在");
        }
        return model;
    }

    private Vehicle requireVehicle(Long id) {
        Vehicle vehicle = vehicleMapper.selectById(id);
        if (vehicle == null || Integer.valueOf(1).equals(vehicle.getDeleted())) {
            throw new BusinessException(ResultCode.NOT_FOUND, "车辆不存在");
        }
        return vehicle;
    }

    private VehicleInsurance requireInsurance(Long id) {
        VehicleInsurance insurance = insuranceMapper.selectById(id);
        if (insurance == null || Integer.valueOf(1).equals(insurance.getDeleted())) {
            throw new BusinessException(ResultCode.NOT_FOUND, "保单不存在");
        }
        return insurance;
    }

    private BrandQueryDTO brandQueryOf(Long id) {
        BrandQueryDTO q = new BrandQueryDTO();
        q.setPage(1);
        q.setSize(50);
        return q;
    }

    /** 首字母缺失时取品牌名首字，保证前台品牌索引有分组可归 */
    private static String resolveInitial(String initial, String name) {
        String value = trimToNull(initial);
        if (value != null) {
            return value.substring(0, Math.min(4, value.length())).toUpperCase();
        }
        String fallback = name.substring(0, 1);
        return fallback.toUpperCase();
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
