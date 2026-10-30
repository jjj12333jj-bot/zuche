package com.zuche.module.user.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 平台用户列表项
 *
 * <p>与信用视角的 {@code AdminUserCreditVO} 不同：那份关注「风险」，
 * 这份关注「账号」——角色、归属网点、账号状态、消费贡献。
 * 两个页面服务两件事，字段重合但用途不同，不合并。</p>
 */
@Data
@Schema(description = "平台用户列表项")
public class AdminUserVO {

    private Long id;

    private String username;

    private String realName;

    private String phone;

    private String email;

    private String avatar;

    /* ===== 信用（只读，改动走信用调整接口） ===== */

    private Integer creditScore;

    private String creditLevel;

    private String levelLabel;

    @Schema(description = "等级配色，前端直接用，避免各处再写一遍映射")
    private String levelColor;

    /* ===== 账号 ===== */

    @Schema(description = "角色码 USER / STORE_ADMIN / SYS_ADMIN")
    private String roleCode;

    private String roleName;

    private Long storeId;

    private String storeName;

    @Schema(description = "1 正常 0 已冻结")
    private Integer status;

    private String statusLabel;

    private LocalDateTime lastLoginTime;

    private LocalDateTime createTime;

    /* ===== 消费 ===== */

    @Schema(description = "累计订单数（不含已取消）")
    private Integer orderCount;

    @Schema(description = "已完成订单数")
    private Integer finishedCount;

    @Schema(description = "当前进行中的订单数（待取车/在租/逾期）")
    private Integer activeCount;

    @Schema(description = "累计消费金额（不含待支付与已取消）")
    private BigDecimal totalSpend;
}
