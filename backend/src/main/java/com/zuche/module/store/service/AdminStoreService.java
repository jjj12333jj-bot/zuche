package com.zuche.module.store.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zuche.common.BusinessException;
import com.zuche.common.PageResult;
import com.zuche.common.ResultCode;
import com.zuche.module.store.dto.AdminStoreQueryDTO;
import com.zuche.module.store.dto.StoreSaveDTO;
import com.zuche.module.store.entity.Store;
import com.zuche.module.store.mapper.AdminStoreMapper;
import com.zuche.module.store.mapper.StoreMapper;
import com.zuche.module.store.vo.AdminStoreStatsVO;
import com.zuche.module.store.vo.AdminStoreVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 平台网点管理
 *
 * <p>网点是订单、车辆、人员的归属根：删掉一个网点，挂在上面的车和单都会变成孤儿。
 * 因此这里的策略是「能停用、不删除」——只有从未产生过任何车辆与订单的网点才允许物理删除，
 * 用来清理建错的空网点。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminStoreService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private final AdminStoreMapper adminStoreMapper;
    private final StoreMapper storeMapper;

    /* ==================== 查询 ==================== */

    public PageResult<AdminStoreVO> page(AdminStoreQueryDTO query) {
        Page<AdminStoreVO> page = new Page<>(query.getCurrent(), query.getPageSize());
        IPage<AdminStoreVO> result = adminStoreMapper.selectStorePage(page, query);
        result.getRecords().forEach(this::decorate);
        return PageResult.of(result);
    }

    public AdminStoreStatsVO stats() {
        AdminStoreStatsVO vo = adminStoreMapper.selectStats();
        if (vo == null) {
            vo = new AdminStoreStatsVO();
        }
        vo.setTotal(nz(vo.getTotal()));
        vo.setNormal(nz(vo.getNormal()));
        vo.setDisabled(nz(vo.getDisabled()));
        vo.setVehicleTotal(nz(vo.getVehicleTotal()));
        vo.setMonthRevenue(vo.getMonthRevenue() == null ? ZERO : vo.getMonthRevenue());
        return vo;
    }

    public AdminStoreVO detail(Long id) {
        AdminStoreVO vo = adminStoreMapper.selectStoreById(id);
        if (vo == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "网点不存在");
        }
        return decorate(vo);
    }

    /* ==================== 新增 / 编辑 ==================== */

    @Transactional(rollbackFor = Exception.class)
    public AdminStoreVO create(StoreSaveDTO dto) {
        String code = dto.getStoreCode().trim().toUpperCase();
        if (adminStoreMapper.countByCode(code, null) > 0) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "网点编码「" + code + "」已存在");
        }
        Store store = new Store();
        apply(dto, store);
        store.setStoreCode(code);
        store.setCreateTime(LocalDateTime.now());
        store.setUpdateTime(LocalDateTime.now());
        storeMapper.insert(store);

        log.info("新增网点: code={}, name={}", code, store.getStoreName());
        return detail(store.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public AdminStoreVO update(Long id, StoreSaveDTO dto) {
        Store exists = storeMapper.selectById(id);
        if (exists == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "网点不存在");
        }
        String code = dto.getStoreCode().trim().toUpperCase();
        if (adminStoreMapper.countByCode(code, id) > 0) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "网点编码「" + code + "」已被其他网点占用");
        }

        // 停用中的网点改回营业，等同于「恢复营业」，同样要看有没有在途单
        Store update = new Store();
        update.setId(id);
        apply(dto, update);
        update.setStoreCode(code);
        update.setUpdateTime(LocalDateTime.now());
        storeMapper.updateById(update);

        log.info("编辑网点: id={}, code={}", id, code);
        return detail(id);
    }

    /* ==================== 删除 / 停用 ==================== */

    /**
     * 删除网点
     *
     * <p>只有「零车辆 + 零订单」的网点可以删。哪怕只有一台车在册，也要先调走或下架，
     * 否则那台车会挂在一个不存在的网点上，前台按网点筛车时永远查不到它。</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Store exists = storeMapper.selectById(id);
        if (exists == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "网点不存在");
        }
        int vehicles = adminStoreMapper.countVehicles(id);
        int orders = adminStoreMapper.countOrders(id);
        if (vehicles > 0 || orders > 0) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR,
                    "该网点下还有 " + vehicles + " 台车、" + orders + " 笔订单，不能删除；"
                            + "请先把车辆调走或下架，或改为「停用营业」");
        }
        storeMapper.deleteById(id);
        log.info("删除空网点: id={}, code={}", id, exists.getStoreCode());
    }

    /**
     * 停用 / 恢复营业
     *
     * <p>停用前必须确认没有进行中的订单：客户已经付了钱、约好到这家店取车，
     * 店却关门了，只能由客服逐个打电话道歉。</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public AdminStoreVO toggleStatus(Long id, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "状态只能是 0（停用）或 1（营业）");
        }
        Store exists = storeMapper.selectById(id);
        if (exists == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "网点不存在");
        }
        if (status == 0) {
            int active = adminStoreMapper.countActiveOrders(id);
            if (active > 0) {
                throw new BusinessException(ResultCode.BUSINESS_ERROR,
                        "该网点还有 " + active + " 笔进行中的订单（待取车/在租/逾期），不能停用；"
                                + "请等订单结束或先调车处理");
            }
        }
        if (Integer.valueOf(status).equals(exists.getStatus())) {
            return detail(id);
        }
        Store update = new Store();
        update.setId(id);
        update.setStatus(status);
        update.setUpdateTime(LocalDateTime.now());
        storeMapper.updateById(update);

        log.info("网点状态变更: id={}, {} -> {}", id, exists.getStatus(), status);
        return detail(id);
    }

    /* ==================== 内部 ==================== */

    private void apply(StoreSaveDTO dto, Store target) {
        target.setStoreName(dto.getStoreName().trim());
        target.setAddress(dto.getAddress().trim());
        target.setPhone(dto.getPhone().trim());
        target.setBusinessHours(dto.getBusinessHours().trim());
        target.setLongitude(dto.getLongitude());
        target.setLatitude(dto.getLatitude());
        target.setManagerId(dto.getManagerId());
        target.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
    }

    private AdminStoreVO decorate(AdminStoreVO vo) {
        if (vo == null) {
            return null;
        }
        vo.setStatusLabel(Integer.valueOf(1).equals(vo.getStatus()) ? "营业中" : "已停用");
        vo.setVehicleTotal(nz(vo.getVehicleTotal()));
        vo.setVehicleAvailable(nz(vo.getVehicleAvailable()));
        vo.setVehicleRented(nz(vo.getVehicleRented()));
        vo.setVehicleMaintenance(nz(vo.getVehicleMaintenance()));
        vo.setOrderTotal(nz(vo.getOrderTotal()));
        vo.setActiveOrderCount(nz(vo.getActiveOrderCount()));
        vo.setMonthRevenue(vo.getMonthRevenue() == null ? ZERO : vo.getMonthRevenue());
        return vo;
    }

    private static int nz(Integer value) {
        return value == null ? 0 : value;
    }
}
