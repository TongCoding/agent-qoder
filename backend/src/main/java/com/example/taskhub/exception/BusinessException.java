package com.example.taskhub.exception;

import com.example.taskhub.common.ErrorCode;
import lombok.Getter;

import java.io.Serial;

/**
 * 业务异常基类。
 *
 * <p>业务代码中所有可预期的错误（如资源不存在、状态冲突、唯一性校验失败）都应抛出该异常，
 * 由 {@link GlobalExceptionHandler} 统一转换为标准响应结构。</p>
 *
 * <p>使用示例：</p>
 * <pre>{@code
 * throw new BusinessException(ErrorCode.USERNAME_DUPLICATED);
 * throw new BusinessException(ErrorCode.CONFLICT, "任务已完成，无法再次流转");
 * }</pre>
 *
 * @author TaskHub
 */
@Getter
public class BusinessException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 业务错误码 */
    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    /**
     * 抛出一个「资源不存在」异常。
     *
     * @param errorCode 具体的资源错误码
     * @param id        资源主键
     */
    public static BusinessException notFound(ErrorCode errorCode, Object id) {
        return new BusinessException(errorCode, errorCode.getMessage() + "，ID = " + id);
    }
}
