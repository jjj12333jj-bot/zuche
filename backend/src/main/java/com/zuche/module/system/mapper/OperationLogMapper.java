package com.zuche.module.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zuche.module.system.dto.OperationLogQueryDTO;
import com.zuche.module.system.entity.OperationLog;
import com.zuche.module.system.vo.OperationLogStatsVO;
import com.zuche.module.system.vo.OperationLogVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 操作日志查询（只读）
 */
@Mapper
public interface OperationLogMapper extends BaseMapper<OperationLog> {

    IPage<OperationLogVO> selectLogPage(Page<OperationLogVO> page, @Param("q") OperationLogQueryDTO query);

    OperationLogStatsVO selectStats();

    /** 已出现过的模块清单，供筛选下拉使用 */
    List<String> selectModules();
}
