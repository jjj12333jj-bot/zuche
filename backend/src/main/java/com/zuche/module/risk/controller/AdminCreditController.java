package com.zuche.module.risk.controller;

import com.zuche.common.PageResult;
import com.zuche.common.Result;
import com.zuche.module.credit.dto.CreditLogQueryDTO;
import com.zuche.module.credit.dto.CreditRuleUpdateDTO;
import com.zuche.module.credit.dto.LevelRuleUpdateDTO;
import com.zuche.module.credit.service.CreditService;
import com.zuche.module.credit.vo.CreditLogVO;
import com.zuche.module.credit.vo.CreditRuleVO;
import com.zuche.module.risk.dto.AdminUserQueryDTO;
import com.zuche.module.risk.dto.AppealQueryDTO;
import com.zuche.module.risk.dto.BlacklistAddDTO;
import com.zuche.module.risk.dto.BlacklistQueryDTO;
import com.zuche.module.risk.dto.CreditAdjustDTO;
import com.zuche.module.risk.dto.CreditAppealHandleDTO;
import com.zuche.module.risk.service.RiskControlService;
import com.zuche.module.risk.vo.AdminUserCreditVO;
import com.zuche.module.risk.vo.CreditAppealVO;
import com.zuche.module.risk.vo.CreditBlacklistVO;
import com.zuche.security.RequireRole;
import com.zuche.security.RoleCode;
import com.zuche.security.UserContext;
import io.swagger.v3.oas.annotations.Operation;
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

import java.util.List;
import java.util.Map;

/**
 * 后台信用风控管理（仅系统管理员）
 *
 * <p>类级 {@link RequireRole} 一次性覆盖下面所有接口，不存在"某个方法忘了加注解"的漏网之鱼。
 * 前端菜单隐藏只是体验，真正的权限边界在这里：普通用户与网点管理员访问一律 403。</p>
 */
@Tag(name = "05-后台信用风控", description = "规则配置、用户调分、申诉审批、风控名单")
@RestController
@RequestMapping("/api/admin/credit")
@RequiredArgsConstructor
@RequireRole(RoleCode.SYS_ADMIN)
@SecurityRequirement(name = "Authorization")
public class AdminCreditController {

    private final CreditService creditService;
    private final RiskControlService riskControlService;

    /* ==================== 规则配置 ==================== */

    @Operation(summary = "规则列表", description = "ruleType 为空查全部，可按 LEVEL/ACTION/PARAM 过滤，含停用项")
    @GetMapping("/rules")
    public Result<List<CreditRuleVO>> rules(String ruleType) {
        return Result.success(creditService.listRules(ruleType));
    }

    @Operation(summary = "修改事件规则或风控参数", description = "等级规则请用整组保存接口")
    @PutMapping("/rules/{id}")
    public Result<CreditRuleVO> updateRule(@PathVariable Long id,
                                           @Valid @RequestBody CreditRuleUpdateDTO dto) {
        return Result.success("规则已更新", creditService.updateRule(id, dto));
    }

    @Operation(summary = "整组保存等级规则",
            description = "四档区间必须从 0 分起、首尾相接；保存后自动重算全部用户等级")
    @PutMapping("/rules/levels")
    public Result<List<CreditRuleVO>> updateLevels(@Valid @RequestBody List<LevelRuleUpdateDTO> rules) {
        return Result.success("等级规则已保存，用户等级已重算", creditService.updateLevelRules(rules));
    }

    @Operation(summary = "重算全部用户等级", description = "按当前 LEVEL 规则重新落库 credit_level")
    @PostMapping("/rules/recalc")
    public Result<Map<String, Object>> recalcLevels() {
        int changed = creditService.recalcAllLevels();
        return Result.success("已重算，变更 " + changed + " 人", Map.of("changed", changed));
    }

    /* ==================== 用户信用 ==================== */

    @Operation(summary = "用户信用分页", description = "按用户名/姓名/手机号搜索，可按等级或风控状态过滤")
    @GetMapping("/users")
    public Result<PageResult<AdminUserCreditVO>> users(@ParameterObject @ModelAttribute AdminUserQueryDTO query) {
        return Result.success(riskControlService.pageUsers(query));
    }

    @Operation(summary = "人工调整信用分", description = "原因必填；写入 change_type=ADMIN 的信用日志")
    @PostMapping("/users/{userId}/adjust")
    public Result<Map<String, Object>> adjust(@PathVariable Long userId,
                                              @Valid @RequestBody CreditAdjustDTO dto) {
        var change = creditService.adjustByAdmin(userId, dto.getScoreChange(),
                dto.getReason(), UserContext.getUserId());
        return Result.success("调整成功：" + change.getScoreBefore() + " → " + change.getScoreAfter(),
                Map.of("change", change));
    }

    @Operation(summary = "指定用户的信用明细", description = "后台查看某用户的全部加减分记录")
    @GetMapping("/users/{userId}/logs")
    public Result<PageResult<CreditLogVO>> userLogs(@PathVariable Long userId,
                                                    @ParameterObject @ModelAttribute CreditLogQueryDTO query) {
        return Result.success(creditService.pageLogs(userId, query));
    }

    /* ==================== 申诉处理 ==================== */

    @Operation(summary = "申诉分页", description = "status 为空查全部；关键词支持申诉单号/用户名/订单号")
    @GetMapping("/appeals")
    public Result<PageResult<CreditAppealVO>> appeals(@ParameterObject @ModelAttribute AppealQueryDTO query) {
        return Result.success(riskControlService.pageAppeals(query));
    }

    @Operation(summary = "申诉审批", description = "通过则补分并落 APPEAL 日志，驳回只改状态")
    @PostMapping("/appeals/{id}/handle")
    public Result<CreditAppealVO> handleAppeal(@PathVariable Long id,
                                               @Valid @RequestBody CreditAppealHandleDTO dto) {
        CreditAppealVO vo = riskControlService.handleAppeal(id, dto, UserContext.getUserId());
        return Result.success("已通过".equals(vo.getStatusLabel()) ? "申诉通过，分数已补回" : "申诉已驳回", vo);
    }

    /* ==================== 风控名单 ==================== */

    @Operation(summary = "风控名单分页", description = "status/listType/keyword 组合筛选")
    @GetMapping("/blacklist")
    public Result<PageResult<CreditBlacklistVO>> blacklist(@ParameterObject @ModelAttribute BlacklistQueryDTO query) {
        return Result.success(riskControlService.pageBlacklist(query));
    }

    @Operation(summary = "手工加入风控名单", description = "WARNING 只预警，OVERRIDE/BLACKLIST 会拦截下单")
    @PostMapping("/blacklist")
    public Result<Void> addBlacklist(@Valid @RequestBody BlacklistAddDTO dto) {
        riskControlService.addBlacklist(dto, UserContext.getUserId());
        return Result.success("已加入风控名单", null);
    }

    @Operation(summary = "解除风控名单")
    @PostMapping("/blacklist/{id}/release")
    public Result<Void> releaseBlacklist(@PathVariable Long id) {
        riskControlService.releaseBlacklist(id, UserContext.getUserId());
        return Result.success("已解除", null);
    }
}
