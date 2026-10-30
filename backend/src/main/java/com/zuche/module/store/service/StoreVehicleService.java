package com.zuche.module.store.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zuche.common.BusinessException;
import com.zuche.common.PageResult;
import com.zuche.common.ResultCode;
import com.zuche.module.store.dto.MaintenanceCreateDTO;
import com.zuche.module.store.dto.MaintenanceQueryDTO;
import com.zuche.module.store.dto.StoreVehicleQueryDTO;
import com.zuche.module.store.dto.VehiclePriceDTO;
import com.zuche.module.store.dto.VehicleStatusDTO;
import com.zuche.module.store.entity.VehicleMaintenance;
import com.zuche.module.store.mapper.StoreVehicleMapper;
import com.zuche.module.store.mapper.VehicleMaintenanceMapper;
import com.zuche.module.store.vo.MaintenanceVO;
import com.zuche.module.store.vo.StoreVehicleVO;
import com.zuche.module.vehicle.entity.Vehicle;
import com.zuche.module.vehicle.mapper.VehicleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

/**
 * 网点车辆管理
 *
 * <p>三条硬规则，都写在这一层，不依赖前端禁用按钮：</p>
 * <ol>
 *   <li><b>只能动自己店里的车</b>——查不到就 404，不区分「不存在」和「不是你的」，
 *       否则网点号可以被枚举出来。</li>
 *   <li><b>车辆可用性以 {@code vehicle.status} 为准</b>——支付成功锁车置 2、归还完成置 1。
 *       本模块所有"这车能不能动"的判断都看这个字段，不去反推订单状态：
 *       种子数据里存在"可租但仍有历史未结束订单"的情况，若按订单判定，
 *       24 台车会全部被判为占用，整店无车可操作。这一点在 docs 里如实记录。</li>
 *   <li><b>状态 2（已租出）不能手工改</b>——由支付与归还自动维护。
 *       手工改成别的值，车还在客户手上就从可租列表里消失，或反之超卖。</li>
 *   <li><b>价格可以随时改</b>——订单在下单时已快照单价，改价只影响新订单。</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
public class StoreVehicleService {

    /** 0 下架 1 可租 2 已租出 3 维保中 */
    private static final Map<Integer, String> STATUS_LABELS = Map.of(
            0, "已下架",
            1, "可租",
            2, "已租出",
            3, "维保中");

    /** 允许手工设置的状态：2 不在其中 */
    private static final Set<Integer> MANUAL_STATUS = Set.of(0, 1, 3);

    private static final Map<String, String> MAINT_LABELS = Map.of(
            "MAINTAIN", "保养",
            "REPAIR", "维修");

    private static final Map<String, String> ORDER_STATUS_LABELS = Map.of(
            "WAIT_PICKUP", "待交付",
            "IN_USE", "在租",
            "WAIT_RETURN", "待归还",
            "OVERDUE", "逾期");

    private final StoreVehicleMapper storeVehicleMapper;
    private final VehicleMapper vehicleMapper;
    private final VehicleMaintenanceMapper maintenanceMapper;

    /* ==================== 车辆 ==================== */

    /** 本网点车辆分页（含台账摘要与当前占用订单） */
    public PageResult<StoreVehicleVO> vehicles(StoreVehicleQueryDTO query) {
        Long storeId = StoreContext.requireStoreId();
        query.setKeyword(trimToNull(query.getKeyword()));

        Page<StoreVehicleVO> page = new Page<>(query.getCurrent(), query.getPageSize());
        IPage<StoreVehicleVO> result = storeVehicleMapper.selectVehiclePage(page, storeId, query);
        result.getRecords().forEach(this::decorate);
        return PageResult.of(result);
    }

    /** 调价：只改价格，不动状态和归属，因此有未结束订单也允许 */
    @Transactional(rollbackFor = Exception.class)
    public String updatePrice(Long vehicleId, VehiclePriceDTO dto) {
        Vehicle vehicle = requireOwnVehicleForUpdate(vehicleId);

        String oldRent = money(vehicle.getDailyPrice());
        String oldDeposit = money(vehicle.getDeposit());

        Vehicle update = new Vehicle();
        update.setId(vehicle.getId());
        update.setDailyPrice(dto.getDailyPrice());
        update.setDeposit(dto.getDeposit());
        vehicleMapper.updateById(update);

        return "「" + vehicle.getLicensePlate() + "」已改价：日租金 " + oldRent + " → " + money(dto.getDailyPrice())
                + "，押金 " + oldDeposit + " → " + money(dto.getDeposit())
                + "。已成交订单按下单时的价格结算，不受影响。";
    }

    /** 上下架 / 进维保 / 恢复可租 */
    @Transactional(rollbackFor = Exception.class)
    public String updateStatus(Long vehicleId, VehicleStatusDTO dto) {
        Integer target = dto.getStatus();
        if (target == null || !MANUAL_STATUS.contains(target)) {
            throw new BusinessException(ResultCode.BAD_REQUEST,
                    "状态只能设置为 0 下架 / 1 可租 / 3 维保中；已租出由系统按订单自动维护");
        }

        Vehicle vehicle = requireOwnVehicleForUpdate(vehicleId);
        Integer current = vehicle.getStatus();

        if (target.equals(current)) {
            throw new BusinessException(ResultCode.BAD_REQUEST,
                    "该车辆当前已是「" + label(current) + "」，无需重复设置");
        }

        // 车辆可用性以 vehicle.status 为准（支付成功置 2、归还完成置 1）。
        // 已租出的车必须先走订单归还，否则会出现「车还在客户手上却被下架」。
        if (current != null && current == 2) {
            throw new BusinessException(ResultCode.BAD_REQUEST,
                    "该车辆当前为「已租出」，需先在订单里办理归还，才能调整状态");
        }

        Vehicle update = new Vehicle();
        update.setId(vehicleId);
        update.setStatus(target);
        vehicleMapper.updateById(update);

        return "「" + vehicle.getLicensePlate() + "」状态：" + label(current) + " → " + label(target) + tailOf(target);
    }

    /* ==================== 台账 ==================== */

    /** 本网点台账分页（可按车辆/类型/关键词过滤） */
    public PageResult<MaintenanceVO> maintenances(MaintenanceQueryDTO query) {
        Long storeId = StoreContext.requireStoreId();
        query.setKeyword(trimToNull(query.getKeyword()));

        Page<MaintenanceVO> page = new Page<>(query.getCurrent(), query.getPageSize());
        IPage<MaintenanceVO> result = storeVehicleMapper.selectMaintenancePage(page, storeId, query);
        result.getRecords().forEach(vo -> vo.setMaintTypeLabel(
                MAINT_LABELS.getOrDefault(vo.getMaintType(), vo.getMaintType())));
        return PageResult.of(result);
    }

    /**
     * 登记台账
     *
     * <p>两个连带动作：</p>
     * <ul>
     *   <li>进场里程大于车辆当前里程时，顺手把车辆里程往前推——台账是里程的唯一可信来源，
     *       让人再单独去改一次车辆档案，迟早会忘。</li>
     *   <li>登记「维修」且车辆当前可租、又没有未结束订单时，自动置为维保中。
     *       维修中的车还能被下单是真实的业务事故。</li>
     * </ul>
     */
    @Transactional(rollbackFor = Exception.class)
    public String addMaintenance(Long vehicleId, MaintenanceCreateDTO dto) {
        Vehicle vehicle = requireOwnVehicleForUpdate(vehicleId);

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
        record.setOperatorId(StoreContext.requireUserId());
        maintenanceMapper.insert(record);

        StringBuilder message = new StringBuilder("台账已登记（")
                .append(MAINT_LABELS.getOrDefault(dto.getMaintType(), dto.getMaintType()))
                .append(" · ").append(dto.getMaintDate()).append("）");

        // 里程只增不减，防止把更大的档案值改小
        if (dto.getMileage() != null
                && (vehicle.getCurrentMileage() == null || dto.getMileage().compareTo(vehicle.getCurrentMileage()) > 0)) {
            Vehicle mileageUpdate = new Vehicle();
            mileageUpdate.setId(vehicleId);
            mileageUpdate.setCurrentMileage(dto.getMileage());
            vehicleMapper.updateById(mileageUpdate);
            message.append("，车辆里程已更新为 ").append(dto.getMileage()).append(" 万公里");
        }

        if ("REPAIR".equals(dto.getMaintType())
                && vehicle.getStatus() != null && vehicle.getStatus() == 1) {
            Vehicle statusUpdate = new Vehicle();
            statusUpdate.setId(vehicleId);
            statusUpdate.setStatus(3);
            vehicleMapper.updateById(statusUpdate);
            message.append("，车辆已自动置为维保中，暂不可租");
        }

        return message.append("。").toString();
    }

    /* ==================== 内部 ==================== */

    /** 行锁 + 归属校验：写操作统一入口 */
    private Vehicle requireOwnVehicleForUpdate(Long vehicleId) {
        Long storeId = StoreContext.requireStoreId();
        Vehicle vehicle = storeVehicleMapper.selectVehicleForUpdate(vehicleId);
        // 不区分「不存在」与「不是你的」，避免通过返回码枚举别家网点的车辆 ID
        if (vehicle == null || !storeId.equals(vehicle.getStoreId())) {
            throw new BusinessException(ResultCode.NOT_FOUND, "本网点不存在该车辆");
        }
        return vehicle;
    }

    private void decorate(StoreVehicleVO vo) {
        vo.setStatusLabel(label(vo.getStatus()));
        // 已租出 == 被占用，与 vehicle.status 保持同一个口径
        boolean occupied = vo.getStatus() != null && vo.getStatus() == 2;
        vo.setOccupied(occupied);
        if (occupied) {
            vo.setActiveOrderStatusLabel(
                    ORDER_STATUS_LABELS.getOrDefault(vo.getActiveOrderStatus(), vo.getActiveOrderStatus()));
        }
    }

    private static String label(Integer status) {
        if (status == null) {
            return "未知";
        }
        return STATUS_LABELS.getOrDefault(status, "未知");
    }

    private static String tailOf(Integer target) {
        return switch (target) {
            case 3 -> "，该车已退出可租列表";
            case 1 -> "，该车重新对外可租";
            case 0 -> "，该车已下架，不再对外展示";
            default -> "";
        };
    }

    private static String money(BigDecimal value) {
        return value == null ? "—" : "¥" + value.setScale(2, java.math.RoundingMode.HALF_UP);
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
