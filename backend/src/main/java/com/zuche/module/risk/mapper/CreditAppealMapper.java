package com.zuche.module.risk.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zuche.module.risk.entity.CreditAppeal;
import com.zuche.module.risk.vo.CreditAppealVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 信用申诉 Mapper
 */
@Mapper
public interface CreditAppealMapper extends BaseMapper<CreditAppeal> {

    /**
     * 申诉分页：后台按状态/关键词查全部，用户端传 userId 只看自己的
     */
    IPage<CreditAppealVO> selectAppealPage(Page<CreditAppealVO> page,
                                           @Param("userId") Long userId,
                                           @Param("status") String status,
                                           @Param("keyword") String keyword);

    /** 单条申诉（提交后回显、审批后回显） */
    CreditAppealVO selectAppealVO(@Param("id") Long id);

    /** 同一条扣分记录是否已有待处理申诉，防止用户重复提交 */
    @Select("SELECT COUNT(*) FROM credit_appeal "
            + "WHERE credit_log_id = #{creditLogId} AND user_id = #{userId} "
            + "AND status = 'PENDING' AND deleted = 0")
    int countPending(@Param("creditLogId") Long creditLogId, @Param("userId") Long userId);
}
