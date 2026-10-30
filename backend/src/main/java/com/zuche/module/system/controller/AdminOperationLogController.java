package com.zuche.module.system.controller;

import com.zuche.common.PageResult;
import com.zuche.common.Result;
import com.zuche.module.system.dto.OperationLogQueryDTO;
import com.zuche.module.system.service.OperationLogService;
import com.zuche.module.system.vo.OperationLogStatsVO;
import com.zuche.module.system.vo.OperationLogVO;
import com.zuche.security.RequireRole;
import com.zuche.security.RoleCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 操作日志查询（仅系统管理员，只读）
 *
 * <p>刻意不提供修改与删除接口：审计日志一旦可以事后编辑，就不再具备审计价值。</p>
 */
@Tag(name = "16-操作日志", description = "后台写操作留痕：分页查询、统计、模块清单、详情")
@RestController
@RequestMapping("/api/admin/logs")
@RequiredArgsConstructor
@RequireRole(RoleCode.SYS_ADMIN)
@SecurityRequirement(name = "Authorization")
public class AdminOperationLogController {

    private final OperationLogService operationLogService;

    @Operation(summary = "日志统计", description = "总条数 / 今日操作 / 失败数 / 平均耗时")
    @GetMapping("/stats")
    public Result<OperationLogStatsVO> stats() {
        return Result.success(operationLogService.stats());
    }

    @Operation(summary = "模块清单", description = "已出现过的业务模块，供筛选下拉")
    @GetMapping("/modules")
    public Result<List<String>> modules() {
        return Result.success(operationLogService.modules());
    }

    @Operation(summary = "日志分页", description = "按 id 倒序，最新操作在最前")
    @GetMapping
    public Result<PageResult<OperationLogVO>> page(@ParameterObject @ModelAttribute OperationLogQueryDTO query) {
        return Result.success(operationLogService.page(query));
    }

    @Operation(summary = "日志详情", description = "含完整请求参数快照")
    @GetMapping("/{id}")
    public Result<OperationLogVO> detail(@Parameter(description = "日志 ID") @PathVariable Long id) {
        return Result.success(operationLogService.detail(id));
    }
}
