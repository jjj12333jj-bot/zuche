package com.zuche.module.order.dto;

import com.zuche.common.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 评价查询参数
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "评价查询参数")
public class EvaluationQueryDTO extends PageQuery {

    @Schema(description = "评分 1~5，为空查全部")
    private Integer rating;

    @Schema(description = "展示状态：1 已展示 / 0 已隐藏，为空查全部")
    private Integer status;

    @Schema(description = "是否只看未回复：true 只看待回复")
    private Boolean unreplied;

    @Schema(description = "订单号 / 用户名 / 姓名 / 评价内容，模糊匹配")
    private String keyword;

    /**
     * 网点过滤：只由服务端在网点端接口里强制赋值，不接受前端传参
     * （若能传，等于把"看别家网点评价"做成了一个参数）
     */
    @Schema(hidden = true)
    private Long storeId;
}
