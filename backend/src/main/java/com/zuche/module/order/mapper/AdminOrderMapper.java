package com.zuche.module.order.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zuche.module.order.dto.AdminOrderQueryDTO;
import com.zuche.module.order.vo.AdminOrderDetailVO;
import com.zuche.module.order.vo.AdminOrderStatsVO;
import com.zuche.module.order.vo.AdminOrderVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 平台订单查询
 *
 * <p>不放在 {@code LeaseOrderMapper} 里，是因为用户端那份 SQL 带 {@code user_id} 过滤、
 * 网点端那份带 {@code store_id} 过滤，三份条件各不相同。混在一个 XML 里，
 * 后续谁改条件都容易把另一个端的权限过滤改没。</p>
 */
@Mapper
public interface AdminOrderMapper {

    /** 平台订单分页：无归属过滤，可按网点/状态/关键字/取车日期筛 */
    IPage<AdminOrderVO> selectAdminOrderPage(Page<AdminOrderVO> page,
                                             @Param("q") AdminOrderQueryDTO query);

    /** 平台订单详情：同样不限归属 */
    AdminOrderDetailVO selectAdminOrderDetail(@Param("orderId") Long orderId);

    /** 全量统计，供顶部数字卡 */
    AdminOrderStatsVO selectStats();
}
