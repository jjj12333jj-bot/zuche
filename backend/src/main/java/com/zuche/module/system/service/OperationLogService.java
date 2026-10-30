package com.zuche.module.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zuche.common.BusinessException;
import com.zuche.common.PageResult;
import com.zuche.common.ResultCode;
import com.zuche.module.system.dto.OperationLogQueryDTO;
import com.zuche.module.system.entity.OperationLog;
import com.zuche.module.system.mapper.OperationLogMapper;
import com.zuche.module.system.vo.OperationLogStatsVO;
import com.zuche.module.system.vo.OperationLogVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 操作日志（只读 + 切面写入）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OperationLogService {

    private final OperationLogMapper operationLogMapper;

    public PageResult<OperationLogVO> page(OperationLogQueryDTO query) {
        Page<OperationLogVO> page = new Page<>(query.getCurrent(), query.getPageSize());
        IPage<OperationLogVO> result = operationLogMapper.selectLogPage(page, query);
        result.getRecords().forEach(this::decorate);
        return PageResult.of(result);
    }

    public OperationLogStatsVO stats() {
        OperationLogStatsVO vo = operationLogMapper.selectStats();
        if (vo == null) {
            vo = new OperationLogStatsVO();
        }
        vo.setTotal(vo.getTotal() == null ? 0L : vo.getTotal());
        vo.setTodayCount(vo.getTodayCount() == null ? 0L : vo.getTodayCount());
        vo.setFailCount(vo.getFailCount() == null ? 0L : vo.getFailCount());
        vo.setAvgCost(vo.getAvgCost() == null ? 0L : vo.getAvgCost());
        return vo;
    }

    public List<String> modules() {
        return operationLogMapper.selectModules();
    }

    public OperationLogVO detail(Long id) {
        OperationLog entity = operationLogMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "日志不存在");
        }
        OperationLogVO vo = new OperationLogVO();
        vo.setId(entity.getId());
        vo.setUserId(entity.getUserId());
        vo.setUsername(entity.getUsername());
        vo.setModule(entity.getModule());
        vo.setOperation(entity.getOperation());
        vo.setMethod(entity.getMethod());
        vo.setRequestUri(entity.getRequestUri());
        vo.setParams(entity.getParams());
        vo.setIp(entity.getIp());
        vo.setCostMs(entity.getCostMs());
        vo.setStatus(entity.getStatus());
        vo.setCreateTime(entity.getCreateTime());
        return decorate(vo);
    }

    /**
     * 写一条日志
     *
     * <p>审计日志再重要，也不能反过来把业务搞挂：写库失败就在这里吞掉并打 warn，
     * 让「关闭订单成功但日志没记上」只影响审计完整性，不影响这笔业务本身。</p>
     */
    public void record(OperationLog entry) {
        try {
            operationLogMapper.insert(entry);
        } catch (Exception e) {
            log.warn("操作日志写入失败: module={}, operation={}, err={}",
                    entry.getModule(), entry.getOperation(), e.getMessage());
        }
    }

    private OperationLogVO decorate(OperationLogVO vo) {
        vo.setStatusLabel(Integer.valueOf(0).equals(vo.getStatus()) ? "失败" : "成功");
        vo.setCostMs(vo.getCostMs() == null ? 0L : vo.getCostMs());
        return vo;
    }
}
