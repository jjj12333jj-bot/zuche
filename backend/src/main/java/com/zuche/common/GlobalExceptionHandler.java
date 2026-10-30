package com.zuche.common;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import jakarta.validation.ConstraintViolationException;

/**
 * 全局异常处理
 *
 * <p>原则：所有异常都必须转成 Result，前端只处理一种结构。
 * 业务异常的 message 是给用户看的，系统异常只回通用话术并打日志。</p>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 业务异常（可预期，如库存不足、状态不允许） */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusiness(BusinessException e, HttpServletRequest request) {
        log.warn("业务异常 [{}] {} - {}", request.getMethod(), request.getRequestURI(), e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }

    /** @RequestBody 参数校验失败 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValid(MethodArgumentNotValidException e) {
        FieldError fe = e.getBindingResult().getFieldError();
        String msg = fe == null ? "参数校验失败" : fe.getDefaultMessage();
        return Result.fail(ResultCode.BAD_REQUEST, msg);
    }

    /** 表单/Query 参数绑定失败 */
    @ExceptionHandler(BindException.class)
    public Result<Void> handleBind(BindException e) {
        FieldError fe = e.getBindingResult().getFieldError();
        String msg = fe == null ? "参数校验失败" : fe.getDefaultMessage();
        return Result.fail(ResultCode.BAD_REQUEST, msg);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public Result<Void> handleConstraint(ConstraintViolationException e) {
        String msg = e.getConstraintViolations().stream()
                .findFirst().map(v -> v.getMessage()).orElse("参数校验失败");
        return Result.fail(ResultCode.BAD_REQUEST, msg);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result<Void> handleMissingParam(MissingServletRequestParameterException e) {
        return Result.fail(ResultCode.BAD_REQUEST, "缺少必要参数：" + e.getParameterName());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Void> handleNotReadable(HttpMessageNotReadableException e) {
        return Result.fail(ResultCode.BAD_REQUEST, "请求体格式错误");
    }

    /**
     * Query 参数类型不匹配（如 ?days=abc）
     *
     * <p>不单独处理会落到兜底 Exception，前端弹「系统繁忙」，
     * 让人以为服务挂了；实际只是参数写错了，属于 400。</p>
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Result<Void> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        String expect = switch (e.getRequiredType() == null ? "" : e.getRequiredType().getSimpleName()) {
            case "Integer", "Long", "Short", "Double", "Float", "BigDecimal" -> "数字";
            case "Boolean" -> "true 或 false";
            case "LocalDate" -> "日期（yyyy-MM-dd）";
            default -> "合法值";
        };
        return Result.fail(ResultCode.BAD_REQUEST,
                "参数 " + e.getName() + " 格式不正确，应为" + expect);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public Result<Void> handleMethod(HttpRequestMethodNotSupportedException e) {
        return Result.fail(ResultCode.BAD_REQUEST, "不支持的请求方法：" + e.getMethod());
    }

    /**
     * 接口路径不存在
     *
     * <p>不单独处理会落到兜底 Exception，返回 500「系统繁忙」，
     * 导致"URL 写错"和"服务真挂了"在前端看起来一模一样。</p>
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public Result<Void> handleNoResource(NoResourceFoundException e, HttpServletRequest request) {
        log.warn("接口不存在 [{}] {}", request.getMethod(), request.getRequestURI());
        return Result.fail(ResultCode.NOT_FOUND, "接口不存在：" + request.getRequestURI());
    }

    /** 兜底：未预料的异常 */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e, HttpServletRequest request) {
        log.error("系统异常 [{}] {}", request.getMethod(), request.getRequestURI(), e);
        return Result.fail(ResultCode.ERROR);
    }
}
