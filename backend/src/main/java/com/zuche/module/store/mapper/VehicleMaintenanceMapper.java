package com.zuche.module.store.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zuche.module.store.entity.VehicleMaintenance;
import org.apache.ibatis.annotations.Mapper;

/**
 * 车辆台账 Mapper
 *
 * <p>只用到 BaseMapper 的 insert / 条件查询，列表查询走
 * {@link StoreVehicleMapper#selectMaintenancePage}（要联表取车牌）。</p>
 */
@Mapper
public interface VehicleMaintenanceMapper extends BaseMapper<VehicleMaintenance> {
}
