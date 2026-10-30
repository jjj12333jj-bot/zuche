package com.zuche.module.vehicle.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zuche.common.BusinessException;
import com.zuche.common.PageResult;
import com.zuche.common.ResultCode;
import com.zuche.module.vehicle.dto.VehicleQueryDTO;
import com.zuche.module.vehicle.mapper.VehicleMapper;
import com.zuche.module.vehicle.vo.VehicleDetailVO;
import com.zuche.module.vehicle.vo.VehicleImageVO;
import com.zuche.module.vehicle.vo.VehicleListVO;
import com.zuche.module.vehicle.vo.VehicleOptionsVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 车辆浏览服务（公开接口，无需登录）
 */
@Service
@RequiredArgsConstructor
public class VehicleService {

    private final VehicleMapper vehicleMapper;

    public PageResult<VehicleListVO> page(VehicleQueryDTO query) {
        Page<VehicleListVO> page = new Page<>(query.getCurrent(), query.getPageSize());
        IPage<VehicleListVO> result = vehicleMapper.selectVehiclePage(page, query);
        return PageResult.of(result);
    }

    /**
     * 车辆详情：先查详情（过滤下架），命中后再累加浏览量并同步返回值
     */
    public VehicleDetailVO detail(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "车辆 ID 不合法");
        }

        VehicleDetailVO detail = vehicleMapper.selectVehicleDetail(id);
        if (detail == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "车辆不存在或已下架");
        }
        vehicleMapper.increaseViewCount(id);
        detail.setViewCount((detail.getViewCount() == null ? 0 : detail.getViewCount()) + 1);

        List<VehicleImageVO> images = vehicleMapper.selectImages(id);
        // 极端兜底：车辆没有图集记录时，用车型封面作为唯一图片，前端不至于空白
        if (images.isEmpty() && detail.getCoverImage() != null) {
            VehicleImageVO cover = new VehicleImageVO();
            cover.setImageUrl(detail.getCoverImage());
            cover.setImageType("EXTERIOR");
            cover.setSortNo(1);
            images = List.of(cover);
        }
        detail.setImages(images);
        return detail;
    }

    public VehicleOptionsVO options() {
        VehicleOptionsVO options = new VehicleOptionsVO();
        options.setCategories(vehicleMapper.selectCategories());
        options.setBrands(vehicleMapper.selectBrands());
        options.setStores(vehicleMapper.selectStores());
        options.setPriceRange(vehicleMapper.selectPriceRange());
        return options;
    }
}
