package com.zuche.module.order.controller;

import com.zuche.common.PageResult;
import com.zuche.common.Result;
import com.zuche.common.StatusDTO;
import com.zuche.module.order.dto.EvaluationQueryDTO;
import com.zuche.module.order.dto.EvaluationReplyDTO;
import com.zuche.module.order.service.EvaluationService;
import com.zuche.module.order.vo.EvaluationStatsVO;
import com.zuche.module.order.vo.EvaluationVO;
import com.zuche.security.RequireRole;
import com.zuche.security.RoleCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 评价管理（仅系统管理员）
 *
 * <p>评价只隐藏不删除：删掉差评等于自己骗自己，隐藏保留原文还能复盘。</p>
 */
@Tag(name = "11-评价管理", description = "评价查询、回复、展示/隐藏")
@RestController
@RequestMapping("/api/admin/evaluations")
@RequiredArgsConstructor
@RequireRole(RoleCode.SYS_ADMIN)
@SecurityRequirement(name = "Authorization")
public class AdminEvaluationController {

    private final EvaluationService evaluationService;

    @Operation(summary = "评价统计", description = "总数、平均分、星级分布、回复率")
    @GetMapping("/stats")
    public Result<EvaluationStatsVO> stats() {
        return Result.success(evaluationService.stats());
    }

    @Operation(summary = "评价分页", description = "可按评分、状态、是否已回复、关键字筛选")
    @GetMapping
    public Result<PageResult<EvaluationVO>> page(@ParameterObject @ModelAttribute EvaluationQueryDTO query) {
        return Result.success(evaluationService.page(query));
    }

    @Operation(summary = "回复评价", description = "覆盖式回复，重复提交会更新为最新内容")
    @PostMapping("/{id}/reply")
    public Result<EvaluationVO> reply(@Parameter(description = "评价 ID") @PathVariable Long id,
                                      @Valid @RequestBody EvaluationReplyDTO dto) {
        return Result.success("回复成功", evaluationService.reply(id, dto.getReply()));
    }

    @Operation(summary = "展示/隐藏", description = "status=1 展示，0 隐藏；不物理删除")
    @PutMapping("/{id}/status")
    public Result<EvaluationVO> status(@Parameter(description = "评价 ID") @PathVariable Long id,
                                       @Valid @RequestBody StatusDTO dto) {
        return Result.success(evaluationService.toggleStatus(id, dto.getStatus()));
    }
}
