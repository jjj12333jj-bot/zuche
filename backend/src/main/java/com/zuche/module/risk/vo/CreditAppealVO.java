package com.zuche.module.risk.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 后台申诉列表项
 *
 * <p>把"申诉内容"和"被申诉的那条扣分记录"放在同一行：
 * 审核人不需要再点两次去核对原扣分原因，页面本身就是证据链。</p>
 */
@Data
@Schema(description = "信用申诉")
public class CreditAppealVO {

    private Long id;

    private String appealNo;

    private Long userId;

    private String username;

    private String realName;

    @Schema(description = "用户当前信用分")
    private Integer creditScore;

    private String creditLevel;

    private Long creditLogId;

    @Schema(description = "被申诉记录的扣分事件名")
    private String actionName;

    @Schema(description = "被申诉记录的分值变动")
    private Integer logScoreChange;

    @Schema(description = "被申诉记录的说明")
    private String logReason;

    private LocalDateTime logTime;

    private Long orderId;

    private String orderNo;

    private String appealReason;

    /** PENDING / APPROVED / REJECTED */
    private String status;

    private String statusLabel;

    private Long handleUserId;

    private String handleUserName;

    private LocalDateTime handleTime;

    private String handleRemark;

    private Integer restoreScore;

    private LocalDateTime createTime;
}
