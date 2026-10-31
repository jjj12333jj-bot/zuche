package com.zuche.module.order.controller;

import com.zuche.common.Result;
import com.zuche.module.order.dto.EvaluationCreateDTO;
import com.zuche.module.order.service.EvaluationService;
import com.zuche.module.order.vo.EvaluationVO;
import com.zuche.module.order.vo.VehicleEvaluationVO;
import com.zuche.module.system.annotation.OpLog;
import com.zuche.security.RequireRole;
import com.zuche.security.RoleCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 评价（用户端）
 *
 * <p>只有两个接口：写一条评价、看一台车的评价。
 * 写要求登录且必须是自己的已完成订单；看是公开的，游客也能看——
 * 评价的意义就在于让还没下单的人看到真实反馈。</p>
 */
@Tag(name = "06-订单评价", description = "用户发表评价、车辆评价展示")
@RestController
@RequiredArgsConstructor
public class EvaluationController {

    private final EvaluationService evaluationService;

    @Operation(summary = "发表评价",
            description = "订单完成后可评价，一单一评；车辆状况与门店服务分别打分，综合分由后端计算")
    @PostMapping("/api/orders/{id}/evaluation")
    @RequireRole(RoleCode.USER)
    @OpLog(module = "订单评价", operation = "发表评价")
    public Result<EvaluationVO> create(@Parameter(description = "订单 ID") @PathVariable Long id,
                                       @Valid @RequestBody EvaluationCreateDTO dto) {
        return Result.success("评价已提交", evaluationService.create(id, dto));
    }

    @Operation(summary = "车辆评价展示",
            description = "公开接口：返回平均分、双维度均分、星级分布与评价列表；评价人姓名已脱敏")
    @GetMapping("/api/vehicles/{id}/evaluations")
    public Result<VehicleEvaluationVO> vehicleEvaluations(
            @Parameter(description = "车辆 ID") @PathVariable Long id,
            @Parameter(description = "页码，从 1 开始") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页条数，最大 50") @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(evaluationService.vehicleEvaluations(id, page, size));
    }
}
