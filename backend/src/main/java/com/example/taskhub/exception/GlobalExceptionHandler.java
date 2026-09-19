package com.example.taskhub.exception;

import com.example.taskhub.common.ApiResponse;
import com.example.taskhub.common.ErrorCode;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
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

import java.util.List;
import java.util.stream.Collectors;

/**
 * 全局异常处理器。
 *
 * <p>拦截 Controller 层抛出的所有异常，统一转换为 {@link ApiResponse} 结构，
 * 保证前端拿到的响应格式永远一致，同时避免堆栈信息泄漏给客户端。</p>
 *
 * <p>处理原则：</p>
 * <ol>
 *   <li><b>可预期的业务异常</b>：WARN 级别日志（不打堆栈），返回明确的业务错误码；</li>
 *   <li><b>客户端错误</b>（参数、格式、方法）：DEBUG/WARN 级别日志，返回 4xx；</li>
 *   <li><b>未预期异常</b>：ERROR 级别日志（含完整堆栈），返回 500 且隐藏内部细节。</li>
 * </ol>
 *
 * @author TaskHub
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ==================================================================
    // 业务异常
    // ==================================================================

    /**
     * 处理业务异常（资源不存在、状态冲突、唯一性校验失败等）。
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException ex, HttpServletRequest request) {
        ErrorCode errorCode = ex.getErrorCode();
        log.warn("业务异常 | {} {} | code={} | message={}",
                request.getMethod(), request.getRequestURI(), errorCode.getCode(), ex.getMessage());
        return build(errorCode, ex.getMessage(), null);
    }

    /**
     * 处理 JPA 实体未找到异常。
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleEntityNotFound(EntityNotFoundException ex, HttpServletRequest request) {
        log.warn("实体不存在 | {} {} | {}", request.getMethod(), request.getRequestURI(), ex.getMessage());
        return build(ErrorCode.NOT_FOUND, ex.getMessage(), null);
    }

    // ==================================================================
    // 参数校验异常
    // ==================================================================

    /**
     * 处理 {@code @RequestBody} 上的 {@code @Valid} 校验失败。
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<List<FieldValidationError>>> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<FieldValidationError> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(this::toFieldError)
                .toList();
        return validationFailed(request, errors);
    }

    /**
     * 处理表单/查询参数对象（{@code @ModelAttribute}）的绑定与校验失败。
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResponse<List<FieldValidationError>>> handleBindException(
            BindException ex, HttpServletRequest request) {
        List<FieldValidationError> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(this::toFieldError)
                .toList();
        return validationFailed(request, errors);
    }

    /**
     * 处理方法级约束校验失败（Controller 上标注 {@code @Validated} 时触发）。
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<List<FieldValidationError>>> handleConstraintViolation(
            ConstraintViolationException ex, HttpServletRequest request) {
        List<FieldValidationError> errors = ex.getConstraintViolations().stream()
                .map(this::toFieldError)
                .toList();
        return validationFailed(request, errors);
    }

    /**
     * 处理必填查询参数缺失。
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingParameter(
            MissingServletRequestParameterException ex, HttpServletRequest request) {
        String message = String.format("缺少必填参数：%s（类型 %s）", ex.getParameterName(), ex.getParameterType());
        log.warn("参数缺失 | {} {} | {}", request.getMethod(), request.getRequestURI(), message);
        return build(ErrorCode.BAD_REQUEST, message, null);
    }

    /**
     * 处理参数类型不匹配，例如 /todos/abc 中的 abc 无法转为 Long。
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        String requiredType = ex.getRequiredType() == null ? "未知" : ex.getRequiredType().getSimpleName();
        String message = String.format("参数 %s 类型不匹配，期望类型：%s", ex.getName(), requiredType);
        log.warn("参数类型错误 | {} {} | {}", request.getMethod(), request.getRequestURI(), message);
        return build(ErrorCode.TYPE_MISMATCH, message, null);
    }

    /**
     * 处理请求体解析失败（JSON 语法错误、字段类型不符等）。
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleMessageNotReadable(
            HttpMessageNotReadableException ex, HttpServletRequest request) {
        log.warn("请求体解析失败 | {} {} | {}", request.getMethod(), request.getRequestURI(), ex.getMessage());
        return build(ErrorCode.MESSAGE_NOT_READABLE, "请求体格式错误，请检查 JSON 是否合法", null);
    }

    // ==================================================================
    // HTTP 协议层异常
    // ==================================================================

    /**
     * 处理请求方法不支持，例如用 GET 调用只接受 POST 的接口。
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        String message = String.format("不支持 %s 方法，允许的方法：%s", ex.getMethod(),
                ex.getSupportedHttpMethods() == null ? "无" : ex.getSupportedHttpMethods());
        log.warn("请求方法不支持 | {} {}", request.getMethod(), request.getRequestURI());
        return build(ErrorCode.METHOD_NOT_ALLOWED, message, null);
    }

    /**
     * 处理静态资源 / 路由未匹配（Spring 6.1+ 的 404 表现形式）。
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResourceFound(
            NoResourceFoundException ex, HttpServletRequest request) {
        log.debug("资源未找到 | {} {}", request.getMethod(), request.getRequestURI());
        return build(ErrorCode.NOT_FOUND, "请求的接口或资源不存在：" + request.getRequestURI(), null);
    }

    // ==================================================================
    // 数据层异常
    // ==================================================================

    /**
     * 处理数据完整性约束冲突（唯一索引、外键约束、字段长度超限等）。
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolation(
            DataIntegrityViolationException ex, HttpServletRequest request) {
        log.warn("数据约束冲突 | {} {} | {}", request.getMethod(), request.getRequestURI(), ex.getMostSpecificCause().getMessage());
        return build(ErrorCode.CONFLICT, "数据违反完整性约束，请检查提交内容是否重复或超长", null);
    }

    // ==================================================================
    // 兜底异常
    // ==================================================================

    /**
     * 兜底处理所有未被上面方法捕获的异常。
     *
     * <p>注意：这里只记录日志，不把内部异常信息返回给客户端，避免泄漏实现细节。</p>
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("未处理异常 | {} {}", request.getMethod(), request.getRequestURI(), ex);
        return build(ErrorCode.INTERNAL_ERROR, ErrorCode.INTERNAL_ERROR.getMessage(), null);
    }

    // ==================================================================
    // 内部工具方法
    // ==================================================================

    private ResponseEntity<ApiResponse<List<FieldValidationError>>> validationFailed(
            HttpServletRequest request, List<FieldValidationError> errors) {
        String message = errors.isEmpty()
                ? ErrorCode.VALIDATION_FAILED.getMessage()
                : errors.stream().map(FieldValidationError::message).collect(Collectors.joining("；"));
        log.warn("参数校验失败 | {} {} | {}", request.getMethod(), request.getRequestURI(), message);
        return ResponseEntity
                .status(ErrorCode.VALIDATION_FAILED.getHttpStatus())
                .body(ApiResponse.error(ErrorCode.VALIDATION_FAILED, message, errors));
    }

    private FieldValidationError toFieldError(FieldError error) {
        return new FieldValidationError(error.getField(), error.getDefaultMessage(), error.getRejectedValue());
    }

    private FieldValidationError toFieldError(ConstraintViolation<?> violation) {
        String path = violation.getPropertyPath().toString();
        // 方法级校验的 propertyPath 形如 "createTodo.request.title"，只保留最后一段字段名
        String field = path.contains(".") ? path.substring(path.lastIndexOf('.') + 1) : path;
        return new FieldValidationError(field, violation.getMessage(), violation.getInvalidValue());
    }

    private <T> ResponseEntity<ApiResponse<T>> build(ErrorCode errorCode, String message, T data) {
        return ResponseEntity.status(errorCode.getHttpStatus()).body(ApiResponse.error(errorCode, message, data));
    }
}
