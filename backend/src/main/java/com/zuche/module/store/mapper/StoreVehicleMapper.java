package com.zuche.module.store.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zuche.module.store.dto.DispatchQueryDTO;
import com.zuche.module.store.dto.MaintenanceQueryDTO;
import com.zuche.module.store.dto.StoreVehicleQueryDTO;
import com.zuche.module.store.vo.DispatchVO;
import com.zuche.module.store.vo.MaintenanceVO;
import com.zuche.module.store.vo.StoreVehicleVO;
import com.zuche.module.vehicle.entity.Vehicle;
import com.zuche.module.vehicle.vo.StoreOptionVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 网点车辆 / 台账 / 调车 Mapper
 *
 * <p>网点隔离在两个层次都做：SQL 里写死 {@code store_id = #{storeId}}，
 * Service 里再对单条记录校验归属。查询侧拦一道是为了不把越权数据捞出库，
 * 写操作侧拦一道是因为 SQL 的 where 只能证明「这条数据属于我」，不能证明「我该动它」。</p>
 */
@Mapper
public interface StoreVehicleMapper {

    /* ==================== 车辆 ==================== */

    IPage<StoreVehicleVO> selectVehiclePage(Page<StoreVehicleVO> page,
                                            @Param("storeId") Long storeId,
                                            @Param("q") StoreVehicleQueryDTO q);

    /** 行锁取车：改价格、改状态、审批调车之前都要先锁住，避免和支付/归还并发打架 */
    @Select("SELECT * FROM vehicle WHERE id = #{id} AND deleted = 0 FOR UPDATE")
    Vehicle selectVehicleForUpdate(@Param("id") Long id);

    /**
     * 调车审批通过后迁移归属。
     *
     * <p>把 {@code store_id = #{fromStoreId} AND status = 1} 写进 where：
     * 如果申请审批期间车被租走或已被别的申请调走，影响行数为 0，
     * 调用方据此拒绝本次审批，而不是覆盖掉后发生的事实。</p>
     */
    @Update("UPDATE vehicle SET store_id = #{toStoreId}, update_time = NOW() "
            + "WHERE id = #{vehicleId} AND store_id = #{fromStoreId} "
            + "AND status = 1 AND deleted = 0")
    int moveToStore(@Param("vehicleId") Long vehicleId,
                    @Param("fromStoreId") Long fromStoreId,
                    @Param("toStoreId") Long toStoreId);

    /* ==================== 台账 ==================== */

    IPage<MaintenanceVO> selectMaintenancePage(Page<MaintenanceVO> page,
                                               @Param("storeId") Long storeId,
                                               @Param("q") MaintenanceQueryDTO q);

    /* ==================== 调车 ==================== */

    IPage<DispatchVO> selectDispatchPage(Page<DispatchVO> page,
                                         @Param("storeId") Long storeId,
                                         @Param("q") DispatchQueryDTO q);

    /** 当天最大的申请单号，用于生成流水号（含已逻辑删除的行，因为唯一索引仍然管它们） */
    @Select("SELECT MAX(apply_no) FROM dispatch_apply WHERE apply_no LIKE CONCAT(#{prefix}, '%')")
    String selectMaxApplyNo(@Param("prefix") String prefix);

    @Select("SELECT COUNT(*) FROM dispatch_apply WHERE vehicle_id = #{vehicleId} "
            + "AND status = 'PENDING' AND deleted = 0")
    int countPendingApply(@Param("vehicleId") Long vehicleId);

    /* ==================== 网点下拉 ==================== */

    @Select("SELECT s.id, s.store_name AS name, s.address FROM store s "
            + "WHERE s.deleted = 0 AND s.id <> #{excludeId} ORDER BY s.id")
    List<StoreOptionVO> selectOtherStores(@Param("excludeId") Long excludeId);
}
