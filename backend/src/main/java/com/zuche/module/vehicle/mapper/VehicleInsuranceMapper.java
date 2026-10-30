package com.zuche.module.vehicle.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zuche.module.vehicle.entity.VehicleInsurance;
import org.apache.ibatis.annotations.Mapper;

/**
 * 车辆保险单 Mapper
 */
@Mapper
public interface VehicleInsuranceMapper extends BaseMapper<VehicleInsurance> {
}
