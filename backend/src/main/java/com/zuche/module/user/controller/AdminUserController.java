package com.zuche.module.user.controller;

import com.zuche.common.PageResult;
import com.zuche.common.Result;
import com.zuche.common.StatusDTO;
import com.zuche.module.credit.vo.CreditChangeVO;
import com.zuche.module.user.dto.AdminUserQueryDTO;
import com.zuche.module.user.dto.UserCreditAdjustDTO;
import com.zuche.module.user.service.AdminUserService;
import com.zuche.module.user.vo.AdminUserStatsVO;
import com.zuche.module.user.vo.AdminUserVO;
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
 * 平台用户管理（仅系统管理员）
 */
@Tag(name = "12-用户管理", description = "用户查询、冻结/解冻、人工调分")
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@RequireRole(RoleCode.SYS_ADMIN)
@SecurityRequirement(name = "Authorization")
public class AdminUserController {

    private final AdminUserService adminUserService;

    @Operation(summary = "用户统计", description = "总数 / 正常 / 冻结 / 角色分布 / 本月新增")
    @GetMapping("/stats")
    public Result<AdminUserStatsVO> stats() {
        return Result.success(adminUserService.stats());
    }

    @Operation(summary = "用户分页", description = "可按关键字、账号状态、角色、信用等级筛选")
    @GetMapping
    public Result<PageResult<AdminUserVO>> page(@ParameterObject @ModelAttribute AdminUserQueryDTO query) {
        return Result.success(adminUserService.page(query));
    }

    @Operation(summary = "用户详情")
    @GetMapping("/{id}")
    public Result<AdminUserVO> detail(@Parameter(description = "用户 ID") @PathVariable Long id) {
        return Result.success(adminUserService.detail(id));
    }

    @Operation(summary = "冻结/解冻", description = "status=1 正常，0 冻结；冻结后旧 token 立即失效")
    @PutMapping("/{id}/status")
    public Result<AdminUserVO> status(@Parameter(description = "用户 ID") @PathVariable Long id,
                                      @Valid @RequestBody StatusDTO dto) {
        AdminUserVO vo = adminUserService.toggleStatus(id, dto.getStatus());
        return Result.success(Integer.valueOf(1).equals(dto.getStatus()) ? "账号已解冻" : "账号已冻结", vo);
    }

    @Operation(summary = "人工调分", description = "走信用日志，必有原因；单次 -50 ~ +50")
    @PostMapping("/{id}/credit")
    public Result<CreditChangeVO> adjustCredit(@Parameter(description = "用户 ID") @PathVariable Long id,
                                               @Valid @RequestBody UserCreditAdjustDTO dto) {
        CreditChangeVO vo = adminUserService.adjustCredit(id, dto.getScoreChange(), dto.getReason());
        return Result.success("信用分已调整", vo);
    }
}
