package com.zuche.module.store.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zuche.module.store.entity.DispatchApply;
import org.apache.ibatis.annotations.Mapper;

/**
 * 调车申请 Mapper（单条读写走 BaseMapper，列表查询走 StoreVehicleMapper）
 */
@Mapper
public interface DispatchApplyMapper extends BaseMapper<DispatchApply> {
}
