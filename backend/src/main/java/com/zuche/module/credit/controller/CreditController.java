package com.zuche.module.credit.controller;

import com.zuche.common.PageResult;
import com.zuche.common.Result;
import com.zuche.module.credit.dto.CreditLogQueryDTO;
import com.zuche.module.credit.service.CreditService;
import com.zuche.module.credit.vo.CreditCenterVO;
import com.zuche.module.credit.vo.CreditLevelVO;
import com.zuche.module.credit.vo.CreditLogVO;
import com.zuche.module.risk.dto.AppealQueryDTO;
import com.zuche.module.risk.dto.CreditAppealCreateDTO;
import com.zuche.module.risk.service.RiskControlService;
import com.zuche.module.risk.vo.CreditAppealVO;
import com.zuche.security.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 信用中心接口
 *
 * <p>用户侧不提供任何"直接改分"的入口：分数只能由业务动作（支付、取消、归还）
 * 或后台审批触发，避免出现一个可以随便加分的后门。</p>
 *
 * <p>S4 增加申诉：用户可以对某条扣分记录提出异议，但同样不能直接改分，
 * 只能生成一张待审核的申诉单，最终由管理员审批后走信用服务补分。</p>
 *
 * <p>所有查询都按当前登录人过滤，userId 从 token 取，不接受前端传参。</p>
 */
@Tag(name = "04-信用中心", description = "信用概览、押金系数、信用明细")
@RestController
@RequestMapping("/api/credit")
@RequiredArgsConstructor
public class CreditController {

    private final CreditService creditService;
    private final RiskControlService riskControlService;

    @Operation(summary = "信用中心概览", description = "当前分数/等级/押金系数 + 行为统计 + 全部等级规则")
    @GetMapping("/center")
    public Result<CreditCenterVO> center() {
        return Result.success(creditService.getCenter(UserContext.requireUserId()));
    }

    @Operation(summary = "信用明细", description = "按时间倒序分页，可按 changeType 过滤 EVENT/BEHAVIOR")
    @GetMapping("/logs")
    public Result<PageResult<CreditLogVO>> logs(@ParameterObject @ModelAttribute CreditLogQueryDTO query) {
        return Result.success(creditService.pageLogs(UserContext.requireUserId(), query));
    }

    @Operation(summary = "等级规则", description = "四档等级的分数区间、押金系数、颜色，全部来自 credit_rule")
    @GetMapping("/levels")
    public Result<List<CreditLevelVO>> levels() {
        return Result.success(creditService.levelRules(null));
    }

    @Operation(summary = "提交信用申诉", description = "只能对本人真实的扣分记录发起，同一记录只能有一条待处理申诉")
    @PostMapping("/appeals")
    public Result<CreditAppealVO> createAppeal(@Valid @RequestBody CreditAppealCreateDTO dto) {
        return Result.success("申诉已提交，等待管理员处理",
                riskControlService.createAppeal(UserContext.requireUserId(), dto));
    }

    @Operation(summary = "我的申诉", description = "按时间倒序分页")
    @GetMapping("/appeals")
    public Result<PageResult<CreditAppealVO>> myAppeals(@ParameterObject @ModelAttribute AppealQueryDTO query) {
        return Result.success(riskControlService.myAppeals(UserContext.requireUserId(), query));
    }
}
