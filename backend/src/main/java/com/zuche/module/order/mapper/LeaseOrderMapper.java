package com.zuche.module.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zuche.module.order.entity.LeaseOrder;
import com.zuche.module.order.vo.OrderDetailVO;
import com.zuche.module.order.vo.OrderListVO;
import com.zuche.module.store.vo.StoreOrderVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDate;
import java.util.List;

/**
 * 订单 Mapper
 */
@Mapper
public interface LeaseOrderMapper extends BaseMapper<LeaseOrder> {

    /**
     * 行锁读取订单：支付/取消等状态变更必须在事务内先锁行，
     * 防止用户连点两次"支付"生成两组流水。
     */
    @Select("SELECT * FROM lease_order WHERE id = #{id} AND deleted = 0 FOR UPDATE")
    LeaseOrder selectByIdForUpdate(@Param("id") Long id);

    /** 我的订单分页（只查本人，防止越权看到别人的单） */
    IPage<OrderListVO> selectOrderPage(Page<OrderListVO> page,
                                       @Param("userId") Long userId,
                                       @Param("status") String status);

    /**
     * 网点订单分页（只查本网点，网点管理员视角）
     *
     * <p>与 {@link #selectOrderPage} 的区别只在过滤条件：一个是 {@code user_id}，
     * 一个是 {@code store_id}。命中同一个 {@code ListColumns}，
     * 保证两个端看到的字段口径完全一致。</p>
     *
     * @param keyword 订单号 / 用户名 / 手机号，模糊匹配
     */
    IPage<StoreOrderVO> selectStoreOrderPage(Page<StoreOrderVO> page,
                                             @Param("storeId") Long storeId,
                                             @Param("status") String status,
                                             @Param("keyword") String keyword);

    /** 订单详情（带 userId 条件，越权查询直接查不到） */
    OrderDetailVO selectOrderDetail(@Param("orderId") Long orderId,
                                    @Param("userId") Long userId);

    /**
     * 逾期扫描候选：已付过钱、车还没还回来、且已经过了计划还车日
     *
     * <p>覆盖两种真实场景：</p>
     * <ul>
     *   <li>{@code IN_USE}：车开走了没还，最典型的逾期</li>
     *   <li>{@code WAIT_RETURN}：点了"申请归还"却一直没到店确认，车仍在用户手上</li>
     * </ul>
     *
     * <p>用 {@code end_date < 今天} 而不是 {@code <=}：还车日当天 23:59 前仍算履约期内。</p>
     */
    @Select("SELECT * FROM lease_order "
            + "WHERE deleted = 0 AND pay_time IS NOT NULL "
            + "AND status IN ('IN_USE', 'WAIT_RETURN', 'OVERDUE') "
            + "AND end_date < #{today} "
            + "ORDER BY end_date ASC, id ASC")
    List<LeaseOrder> selectOverdueCandidates(@Param("today") LocalDate today);

    /**
     * CAS 置为逾期：{@code WHERE status = 原状态} 保证与用户"确认归还"并发时只有一方成功
     *
     * @return 影响行数，0 表示订单已被其他操作推进，跳过即可
     */
    @Update("UPDATE lease_order SET status = 'OVERDUE', overdue_days = #{overdueDays}, update_time = NOW() "
            + "WHERE id = #{id} AND status = #{fromStatus} AND deleted = 0")
    int markOverdue(@Param("id") Long id,
                    @Param("fromStatus") String fromStatus,
                    @Param("overdueDays") int overdueDays);

    /** 已逾期订单每天刷新逾期天数：天数只增不减，不覆盖人工修正过的更大值 */
    @Update("UPDATE lease_order SET overdue_days = #{overdueDays}, update_time = NOW() "
            + "WHERE id = #{id} AND status = 'OVERDUE' AND deleted = 0 "
            + "AND (overdue_days IS NULL OR overdue_days < #{overdueDays})")
    int refreshOverdueDays(@Param("id") Long id, @Param("overdueDays") int overdueDays);
}
