package com.zuche.module.vehicle.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zuche.module.vehicle.dto.AdminMaintenanceQueryDTO;
import com.zuche.module.vehicle.dto.AdminVehicleQueryDTO;
import com.zuche.module.vehicle.dto.BrandQueryDTO;
import com.zuche.module.vehicle.dto.InsuranceQueryDTO;
import com.zuche.module.vehicle.dto.ModelQueryDTO;
import com.zuche.module.vehicle.entity.Vehicle;
import com.zuche.module.vehicle.vo.AdminMaintenanceVO;
import com.zuche.module.vehicle.vo.AdminVehicleStatsVO;
import com.zuche.module.vehicle.vo.AdminVehicleVO;
import com.zuche.module.vehicle.vo.BrandManageVO;
import com.zuche.module.vehicle.vo.InsuranceVO;
import com.zuche.module.vehicle.vo.ModelManageVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 平台车辆资产 Mapper（品牌 / 车型 / 车辆档案 / 台账 / 保单）
 *
 * <p>全部走 XML：这些列表都要 join 3~4 张表并挂 2 个子查询（车型数、在租订单、保单到期日），
 * 用 Wrapper 拼既不可读也容易漏逻辑删除条件。</p>
 */
@Mapper
public interface AdminVehicleMapper extends BaseMapper<Vehicle> {

    IPage<BrandManageVO> selectBrandPage(Page<BrandManageVO> page, @Param("q") BrandQueryDTO q);

    List<BrandManageVO> selectBrandOptions();

    IPage<ModelManageVO> selectModelPage(Page<ModelManageVO> page, @Param("q") ModelQueryDTO q);

    List<ModelManageVO> selectModelOptions(@Param("brandId") Long brandId);

    IPage<AdminVehicleVO> selectVehiclePage(Page<AdminVehicleVO> page, @Param("q") AdminVehicleQueryDTO q);

    IPage<AdminMaintenanceVO> selectMaintenancePage(Page<AdminMaintenanceVO> page,
                                                    @Param("q") AdminMaintenanceQueryDTO q);

    IPage<InsuranceVO> selectInsurancePage(Page<InsuranceVO> page, @Param("q") InsuranceQueryDTO q);

    /** 全量统计：品牌 / 台账 / 保单，各一次查询，不受分页影响 */
    AdminVehicleStatsVO.BrandStats selectBrandStats();

    AdminVehicleStatsVO.MaintStats selectMaintenanceStats();

    AdminVehicleStatsVO.InsuranceStats selectInsuranceStats();

    /** 某台车是否存在未结束订单：有则不允许下架/删除 */
    int countActiveOrdersOfVehicle(@Param("vehicleId") Long vehicleId);

    /** 车型下挂的车辆数：停用车型前必须先确认这个数 */
    int countVehiclesOfModel(@Param("modelId") Long modelId);

    /** 品牌下挂的车型数：停用品牌前必须先确认这个数 */
    int countModelsOfBrand(@Param("brandId") Long brandId);

    /** 品牌下挂的车辆数：停用时用于提示影响面 */
    int countVehiclesOfBrand(@Param("brandId") Long brandId);

    /** 车辆归属网点，用于校验跨网点操作 */
    Long selectStoreIdOfVehicle(@Param("vehicleId") Long vehicleId);
}
