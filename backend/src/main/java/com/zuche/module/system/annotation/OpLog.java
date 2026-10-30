package com.zuche.module.system.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 操作日志埋点
 *
 * <p>标在后台写接口上，由 {@code OperationLogAspect} 统一记录
 * 「谁、在哪个模块、做了什么、带了什么参数、耗时多久、成没成」。
 * 业务代码里不出现任何一行 log.insert，避免每个方法各写一遍、最后漏掉一半。</p>
 *
 * <pre>
 * &#64;OpLog(module = "公告管理", operation = "发布公告")
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OpLog {

    /** 业务模块，展示在日志列表的「模块」列 */
    String module();

    /** 操作描述，动词开头，如「关闭订单」「冻结用户」 */
    String operation();

    /** 是否记录请求参数；涉及大字段（如公告正文）时可关掉 */
    boolean saveParams() default true;
}
