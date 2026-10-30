package com.zuche.module.vehicle.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zuche.module.vehicle.entity.VehicleBrand;
import org.apache.ibatis.annotations.Mapper;

/**
 * 品牌 Mapper（S3 后台品牌 CRUD 复用）
 */
@Mapper
public interface VehicleBrandMapper extends BaseMapper<VehicleBrand> {
}
