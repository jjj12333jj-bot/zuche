package com.zuche.module.store.service;

import com.zuche.common.BusinessException;
import com.zuche.common.PageResult;
import com.zuche.common.ResultCode;
import com.zuche.module.order.service.OrderService;
import com.zuche.module.order.vo.OrderReturnVO;
import com.zuche.module.store.dto.StoreOrderQueryDTO;
import com.zuche.module.store.mapper.StoreAdminMapper;
import com.zuche.module.store.vo.StoreOrderVO;
import com.zuche.module.store.vo.StoreOverviewVO;
import com.zuche.security.LoginUser;
import com.zuche.security.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 网点工作台
 *
 * <p>职责边界：只做「本网点」的读取与转发。
 * 订单的状态流转、违约金、押金退款、信用结算一律留在 {@link OrderService}，
 * 这里不重复实现——两个入口若各写一套，钱一定算不到一起去。</p>
 */
@Service
@RequiredArgsConstructor
public class StoreAdminService {

    private final StoreAdminMapper storeAdminMapper;
    private final OrderService orderService;

    /** 网点概览：存量指标 + 今日/本月经营量 */
    public StoreOverviewVO overview() {
        Long storeId = requireStoreId();
        StoreOverviewVO vo = storeAdminMapper.selectOverview(storeId);
        if (vo == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "网点不存在或已停用");
        }

        // 比率与派生值放这里算，SQL 只负责捞数
        int total = nz(vo.getVehicleTotal());
        vo.setRentRate(total <= 0 ? null : round(nz(vo.getVehicleRented()) * 100d / total));
        vo.setTodoCount(nz(vo.getPendingPickup()) + nz(vo.getPendingReturn()) + nz(vo.getOverdue()));

        // 空值兜底，前端就不用到处写 ?? 0
        vo.setVehicleTotal(total);
        vo.setVehicleAvailable(nz(vo.getVehicleAvailable()));
        vo.setVehicleRented(nz(vo.getVehicleRented()));
        vo.setVehicleMaintenance(nz(vo.getVehicleMaintenance()));
        vo.setPendingPickup(nz(vo.getPendingPickup()));
        vo.setInUse(nz(vo.getInUse()));
        vo.setPendingReturn(nz(vo.getPendingReturn()));
        vo.setOverdue(nz(vo.getOverdue()));
        vo.setTodayNewOrders(nz(vo.getTodayNewOrders()));
        vo.setTodayCompleted(nz(vo.getTodayCompleted()));
        vo.setMonthOrders(nz(vo.getMonthOrders()));
        vo.setMonthRevenue(vo.getMonthRevenue() == null ? BigDecimal.ZERO : vo.getMonthRevenue());
        vo.setDepositInHand(vo.getDepositInHand() == null ? BigDecimal.ZERO : vo.getDepositInHand());
        return vo;
    }

    /** 本网点订单分页 */
    public PageResult<StoreOrderVO> orders(StoreOrderQueryDTO query) {
        return orderService.storeOrders(query);
    }

    /** 办理交付（待取车 → 已取车） */
    public void pickup(Long orderId) {
        orderService.pickupByStore(orderId);
    }

    /** 办理归还并结算（待归还/逾期 → 已完成） */
    public OrderReturnVO confirmReturn(Long orderId) {
        return orderService.confirmReturnByStore(orderId);
    }

    /* ==================== 工具 ==================== */

    private Long requireStoreId() {
        return StoreContext.requireStoreId();
    }

    private static int nz(Integer value) {
        return value == null ? 0 : value;
    }

    private static Double round(double value) {
        return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }
}
