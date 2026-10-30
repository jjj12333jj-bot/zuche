package com.zuche.module.risk.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zuche.module.risk.entity.CreditBlacklist;
import com.zuche.module.risk.vo.CreditBlacklistVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 风控名单 Mapper
 */
@Mapper
public interface CreditBlacklistMapper extends BaseMapper<CreditBlacklist> {

    IPage<CreditBlacklistVO> selectBlacklistPage(Page<CreditBlacklistVO> page,
                                                 @Param("status") String status,
                                                 @Param("listType") String listType,
                                                 @Param("keyword") String keyword);

    /**
     * 用户当前生效的最高强度风控类型
     *
     * <p>一个用户可能同时有 WARNING 和 BLACKLIST 两条记录，
     * 下单判断只关心最强的那一条，所以按强度排序取第一条。</p>
     */
    @Select("SELECT list_type FROM credit_blacklist "
            + "WHERE user_id = #{userId} AND status = 'ACTIVE' AND deleted = 0 "
            + "AND (expire_time IS NULL OR expire_time > NOW()) "
            + "ORDER BY FIELD(list_type, 'BLACKLIST', 'OVERRIDE', 'WARNING') LIMIT 1")
    String selectActiveType(@Param("userId") Long userId);

    /** 同一用户同类型是否已有生效记录（定时任务幂等：每天跑不会每天插一条） */
    @Select("SELECT COUNT(*) FROM credit_blacklist "
            + "WHERE user_id = #{userId} AND list_type = #{listType} AND status = 'ACTIVE' AND deleted = 0")
    int countActiveType(@Param("userId") Long userId, @Param("listType") String listType);

    /** 到期自动解除 */
    @Update("UPDATE credit_blacklist SET status = 'RELEASED', update_time = NOW() "
            + "WHERE status = 'ACTIVE' AND deleted = 0 "
            + "AND expire_time IS NOT NULL AND expire_time <= NOW()")
    int releaseExpired();
}
