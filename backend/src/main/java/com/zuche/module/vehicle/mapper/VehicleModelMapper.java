package com.zuche.module.vehicle.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zuche.module.vehicle.entity.VehicleModel;
import org.apache.ibatis.annotations.Mapper;

/**
 * 车型 Mapper（S3 后台车型 CRUD 复用）
 */
@Mapper
public interface VehicleModelMapper extends BaseMapper<VehicleModel> {
}
