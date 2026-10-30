package com.zuche.module.store.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zuche.module.store.dto.AdminStoreQueryDTO;
import com.zuche.module.store.vo.AdminStoreStatsVO;
import com.zuche.module.store.vo.AdminStoreVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 平台网点查询
 */
@Mapper
public interface AdminStoreMapper {

    IPage<AdminStoreVO> selectStorePage(Page<AdminStoreVO> page, @Param("q") AdminStoreQueryDTO query);

    AdminStoreVO selectStoreById(@Param("id") Long id);

    AdminStoreStatsVO selectStats();

    /** 编码是否被占用（含已删除，与唯一索引口径一致） */
    int countByCode(@Param("storeCode") String storeCode, @Param("excludeId") Long excludeId);

    /** 网点下车辆数（含各状态），用于删除前的拦截提示 */
    int countVehicles(@Param("storeId") Long storeId);

    /** 网点下订单数，用于删除前的拦截提示 */
    int countOrders(@Param("storeId") Long storeId);

    /** 网点下进行中订单数，用于停用前的拦截 */
    int countActiveOrders(@Param("storeId") Long storeId);
}
