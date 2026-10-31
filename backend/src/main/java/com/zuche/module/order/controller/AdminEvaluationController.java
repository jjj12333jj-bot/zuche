package com.zuche.module.order.controller;

import com.zuche.common.PageResult;
import com.zuche.common.Result;
import com.zuche.common.StatusDTO;
import com.zuche.module.order.dto.EvaluationQueryDTO;
import com.zuche.module.order.dto.EvaluationReplyDTO;
import com.zuche.module.order.service.EvaluationService;
import com.zuche.module.order.vo.EvaluationStatsVO;
import com.zuche.module.order.vo.EvaluationVO;
import com.zuche.module.system.annotation.OpLog;
import com.zuche.security.RequireRole;
import com.zuche.security.RoleCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 评价管理（仅系统管理员）
 *
 * <p>两级处置：轻微不合适的「隐藏」，违规内容才「删除」。删除走逻辑删除留痕，
 * 且只有平台能做——网点作为商家只有回复权，自己删差评等于既当运动员又当裁判。</p>
 */
@Tag(name = "11-评价管理", description = "评价查询、回复、展示/隐藏")
@RestController
@RequestMapping("/api/admin/evaluations")
@RequiredArgsConstructor
@RequireRole(RoleCode.SYS_ADMIN)
@SecurityRequirement(name = "Authorization")
public class AdminEvaluationController {

    private final EvaluationService evaluationService;

    @Operation(summary = "评价统计", description = "总数、综合/车况/服务三项均分、星级分布、回复率")
    @GetMapping("/stats")
    public Result<EvaluationStatsVO> stats() {
        return Result.success(evaluationService.stats(null));
    }

    @Operation(summary = "评价分页", description = "可按评分、状态、是否已回复、关键字筛选")
    @GetMapping
    public Result<PageResult<EvaluationVO>> page(@ParameterObject @ModelAttribute EvaluationQueryDTO query) {
        return Result.success(evaluationService.page(query));
    }

    @Operation(summary = "回复评价", description = "覆盖式回复，重复提交会更新为最新内容")
    @PostMapping("/{id}/reply")
    @OpLog(module = "评价管理", operation = "回复评价")
    public Result<EvaluationVO> reply(@Parameter(description = "评价 ID") @PathVariable Long id,
                                      @Valid @RequestBody EvaluationReplyDTO dto) {
        return Result.success("回复成功", evaluationService.reply(id, dto.getReply()));
    }

    @Operation(summary = "展示/隐藏", description = "status=1 展示，0 隐藏；不删除，随时可恢复")
    @PutMapping("/{id}/status")
    @OpLog(module = "评价管理", operation = "展示/隐藏评价")
    public Result<EvaluationVO> status(@Parameter(description = "评价 ID") @PathVariable Long id,
                                       @Valid @RequestBody StatusDTO dto) {
        return Result.success(evaluationService.toggleStatus(id, dto.getStatus()));
    }

    @Operation(summary = "删除违规评价",
            description = "仅系统管理员可用；逻辑删除，前台与列表都不再出现。网点管理员无此权限")
    @DeleteMapping("/{id}")
    @OpLog(module = "评价管理", operation = "删除评价")
    public Result<Void> remove(@Parameter(description = "评价 ID") @PathVariable Long id,
                               @Parameter(description = "删除原因，可空") @RequestParam(required = false) String reason) {
        evaluationService.remove(id, reason);
        return Result.success("已删除", null);
    }
}
