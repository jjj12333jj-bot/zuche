package com.zuche.module.vehicle.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zuche.module.vehicle.dto.VehicleQueryDTO;
import com.zuche.module.vehicle.entity.Vehicle;
import com.zuche.module.vehicle.vo.BrandOptionVO;
import com.zuche.module.vehicle.vo.CategoryOptionVO;
import com.zuche.module.vehicle.vo.PriceRangeVO;
import com.zuche.module.vehicle.vo.StoreOptionVO;
import com.zuche.module.vehicle.vo.VehicleDetailVO;
import com.zuche.module.vehicle.vo.VehicleImageVO;
import com.zuche.module.vehicle.vo.VehicleListVO;
import com.zuche.module.vehicle.vo.VehicleRentVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 车辆 Mapper
 *
 * <p>列表与详情使用 XML 手写 SQL：涉及 4 张表 join + 2 个字典翻译 + 相关子查询取主图，
 * 用 Wrapper 拼可读性差且容易漏掉逻辑删除条件。</p>
 */
@Mapper
public interface VehicleMapper extends BaseMapper<Vehicle> {

    IPage<VehicleListVO> selectVehiclePage(Page<VehicleListVO> page, @Param("q") VehicleQueryDTO q);

    VehicleDetailVO selectVehicleDetail(@Param("id") Long id);

    List<VehicleImageVO> selectImages(@Param("vehicleId") Long vehicleId);

    List<CategoryOptionVO> selectCategories();

    List<BrandOptionVO> selectBrands();

    List<StoreOptionVO> selectStores();

    PriceRangeVO selectPriceRange();

    /** 下单/试算用车辆信息（含品牌车型名、网点、价格与折扣） */
    VehicleRentVO selectRentVehicle(@Param("id") Long id);

    @Update("UPDATE vehicle SET view_count = view_count + 1 WHERE id = #{id} AND deleted = 0")
    int increaseViewCount(@Param("id") Long id);

    /**
     * 支付成功时锁定车辆：CAS 写法保证并发下只有一笔订单能抢到同一台车
     *
     * <p>用 {@code status = 1} 作为更新条件，返回影响行数 0 即代表车已被别人租走，
     * 调用方必须抛异常回滚整笔支付，而不是"先查再改"（先查再改在两线程下会同时通过检查）。</p>
     */
    @Update("UPDATE vehicle SET status = 2, rent_count = rent_count + 1 "
            + "WHERE id = #{id} AND status = 1 AND deleted = 0")
    int lockForRent(@Param("id") Long id);

    /** 未支付订单取消时释放车辆（只释放自己锁的那台，避免误放维修中的车） */
    @Update("UPDATE vehicle SET status = 1 WHERE id = #{id} AND status = 2 AND deleted = 0")
    int releaseRent(@Param("id") Long id);
}
