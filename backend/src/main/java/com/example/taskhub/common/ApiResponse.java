package com.example.taskhub.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 统一 REST 响应包装体。
 *
 * <p>所有接口（含异常响应）均返回该结构，前端 Axios 拦截器可据此统一处理成功与失败逻辑：</p>
 * <pre>
 * {
 *   "code": 0,
 *   "message": "操作成功",
 *   "data": { ... },
 *   "timestamp": "2026-01-01T10:00:00"
 * }
 * </pre>
 *
 * @param <T> 业务数据类型
 * @author TaskHub
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.ALWAYS)
@Schema(description = "统一响应结构")
public class ApiResponse<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "业务状态码，0 表示成功，非 0 表示失败", example = "0")
    private int code;

    @Schema(description = "提示信息", example = "操作成功")
    private String message;

    @Schema(description = "业务数据，失败时通常为 null")
    private T data;

    @Schema(description = "服务端响应时间（ISO-8601）", example = "2026-01-01T10:00:00")
    private LocalDateTime timestamp;

    // ==================== 成功响应 ====================

    /**
     * 返回成功（无数据体）。
     */
    public static <T> ApiResponse<T> success() {
        return build(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMessage(), null);
    }

    /**
     * 返回成功并携带数据。
     *
     * @param data 业务数据
     */
    public static <T> ApiResponse<T> success(T data) {
        return build(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMessage(), data);
    }

    /**
     * 返回成功，并自定义提示信息。
     *
     * @param message 提示信息
     * @param data    业务数据
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return build(ErrorCode.SUCCESS.getCode(), message, data);
    }

    // ==================== 失败响应 ====================

    /**
     * 根据错误码返回失败响应（使用错误码内置的默认提示）。
     *
     * @param errorCode 错误码枚举
     */
    public static <T> ApiResponse<T> error(ErrorCode errorCode) {
        return build(errorCode.getCode(), errorCode.getMessage(), null);
    }

    /**
     * 根据错误码返回失败响应，并覆盖提示信息。
     *
     * @param errorCode 错误码枚举
     * @param message   自定义提示信息
     */
    public static <T> ApiResponse<T> error(ErrorCode errorCode, String message) {
        return build(errorCode.getCode(), message, null);
    }

    /**
     * 根据错误码返回失败响应，同时携带错误详情（如字段级校验错误）。
     *
     * @param errorCode 错误码枚举
     * @param message   自定义提示信息
     * @param data      错误详情数据
     */
    public static <T> ApiResponse<T> error(ErrorCode errorCode, String message, T data) {
        return build(errorCode.getCode(), message, data);
    }

    // ==================== 内部工具 ====================

    private static <T> ApiResponse<T> build(int code, String message, T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.code = code;
        response.message = message;
        response.data = data;
        response.timestamp = LocalDateTime.now();
        return response;
    }
}
