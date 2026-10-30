package com.zuche.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 分页查询基类
 *
 * <p>page/size 由前端传，但绝不能直接透传给 SQL 的 LIMIT：
 * size 传 100000 就等于给了前端一个全表导出接口。这里统一在 getPageSize() 里收敛上限。</p>
 */
@Data
@Schema(description = "分页查询基类")
public class PageQuery {

    @Schema(description = "页码，从 1 开始", defaultValue = "1")
    private Integer page = 1;

    @Schema(description = "每页条数，最大 50", defaultValue = "10")
    private Integer size = 10;

    public long getCurrent() {
        return page == null || page < 1 ? 1 : page;
    }

    public long getPageSize() {
        if (size == null || size < 1) {
            return 10;
        }
        return Math.min(size, 50);
    }
}
