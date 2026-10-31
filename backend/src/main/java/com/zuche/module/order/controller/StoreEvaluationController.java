package com.zuche.module.order.controller;

import com.zuche.common.PageResult;
import com.zuche.common.Result;
import com.zuche.module.order.dto.EvaluationQueryDTO;
import com.zuche.module.order.dto.EvaluationReplyDTO;
import com.zuche.module.order.service.EvaluationService;
import com.zuche.module.order.vo.EvaluationStatsVO;
import com.zuche.module.order.vo.EvaluationVO;
import com.zuche.module.system.annotation.OpLog;
import com.zuche.module.store.service.StoreContext;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 客户评价（网点端）
 *
 * <p>网点管理员是"商家"，在评价这件事上只有解释权：客人说菜咸了，商家可以说明
 * "已调整配方"，但不能把这条评论撕掉。删除权留给平台，否则每家店都是五星。</p>
 *
 * <p>storeId 一律从 JWT 取，不接受前端传参——能传就能看别家网点。</p>
 */
@Tag(name = "12-客户评价", description = "本网点评价查询与解释性回复")
@RestController
@RequestMapping("/api/store/evaluations")
@RequiredArgsConstructor
@RequireRole(RoleCode.STORE_ADMIN)
@SecurityRequirement(name = "Authorization")
public class StoreEvaluationController {

    private final EvaluationService evaluationService;

    @Operation(summary = "本网点评价统计", description = "综合/车况/服务三项均分、星级分布、待回复数")
    @GetMapping("/stats")
    public Result<EvaluationStatsVO> stats() {
        return Result.success(evaluationService.stats(StoreContext.requireStoreId()));
    }

    @Operation(summary = "本网点评价分页", description = "可按评分、是否已回复、关键字筛选")
    @GetMapping
    public Result<PageResult<EvaluationVO>> page(@ParameterObject @ModelAttribute EvaluationQueryDTO query) {
        // 强制覆盖：前端传什么都无效，只能看本网点
        query.setStoreId(StoreContext.requireStoreId());
        return Result.success(evaluationService.page(query));
    }

    @Operation(summary = "回复评价", description = "覆盖式回复；只能回复本店评价，删除请走平台")
    @PostMapping("/{id}/reply")
    @OpLog(module = "客户评价", operation = "回复评价")
    public Result<EvaluationVO> reply(@Parameter(description = "评价 ID") @PathVariable Long id,
                                      @Valid @RequestBody EvaluationReplyDTO dto) {
        Long storeId = StoreContext.requireStoreId();
        return Result.success("回复成功", evaluationService.replyAsStore(id, dto.getReply(), storeId));
    }
}
