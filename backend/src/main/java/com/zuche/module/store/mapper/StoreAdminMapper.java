package com.zuche.module.store.mapper;

import com.zuche.module.store.vo.StoreOverviewVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 网点端统计 Mapper
 *
 * <p>只做聚合，不算比率——比率统一放 Service，口径要改时改一处。</p>
 */
@Mapper
public interface StoreAdminMapper {

    /** 网点概览：一次查询返回全部存量指标 */
    StoreOverviewVO selectOverview(@Param("storeId") Long storeId);
}
