package com.example.taskhub.common;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 全局业务错误码定义。
 *
 * <p>约定：业务码在 HTTP 状态码基础上扩展为 5 位数字，前三位与 HTTP 状态码保持一致，
 * 后两位用于区分同一类错误下的具体场景，便于前端做精细化处理与日志排查。</p>
 *
 * <pre>
 *   0        -> 成功
 *   400xx    -> 客户端请求错误
 *   404xx    -> 资源不存在
 *   409xx    -> 资源冲突
 *   500xx    -> 服务端内部错误
 * </pre>
 *
 * @author TaskHub
 */
@Getter
public enum ErrorCode {

    /** 请求成功 */
    SUCCESS(0, "操作成功", HttpStatus.OK),

    // ----------------------- 4xx 客户端错误 -----------------------
    /** 请求参数格式错误或缺失 */
    BAD_REQUEST(40000, "请求参数错误", HttpStatus.BAD_REQUEST),
    /** Bean Validation 校验未通过 */
    VALIDATION_FAILED(40001, "参数校验失败", HttpStatus.BAD_REQUEST),
    /** 请求体无法解析（JSON 格式错误等） */
    MESSAGE_NOT_READABLE(40002, "请求体格式错误，无法解析", HttpStatus.BAD_REQUEST),
    /** 路径参数类型不匹配 */
    TYPE_MISMATCH(40003, "参数类型不匹配", HttpStatus.BAD_REQUEST),
    /** 请求方法不被支持 */
    METHOD_NOT_ALLOWED(40500, "不支持的请求方法", HttpStatus.METHOD_NOT_ALLOWED),

    /** 资源不存在（通用） */
    NOT_FOUND(40400, "请求的资源不存在", HttpStatus.NOT_FOUND),
    /** 待办事项不存在 */
    TODO_NOT_FOUND(40401, "待办事项不存在", HttpStatus.NOT_FOUND),
    /** 用户不存在 */
    USER_NOT_FOUND(40402, "用户不存在", HttpStatus.NOT_FOUND),

    /** 资源状态冲突（如唯一约束、非法状态流转） */
    CONFLICT(40900, "资源状态冲突", HttpStatus.CONFLICT),
    /** 用户名已存在 */
    USERNAME_DUPLICATED(40901, "用户名已存在", HttpStatus.CONFLICT),
    /** 邮箱已被占用 */
    EMAIL_DUPLICATED(40902, "邮箱已被占用", HttpStatus.CONFLICT),
    /** 存在关联数据，禁止删除 */
    RESOURCE_IN_USE(40903, "该资源存在关联数据，无法删除", HttpStatus.CONFLICT),

    // ----------------------- 5xx 服务端错误 -----------------------
    /** 未预期的内部异常 */
    INTERNAL_ERROR(50000, "服务器内部错误，请稍后重试", HttpStatus.INTERNAL_SERVER_ERROR),
    /** 第三方服务或依赖调用失败 */
    SERVICE_UNAVAILABLE(50300, "依赖服务暂不可用", HttpStatus.SERVICE_UNAVAILABLE);

    /** 业务错误码，0 表示成功 */
    private final int code;

    /** 默认错误提示，可直接展示给用户 */
    private final String message;

    /** 对应的 HTTP 状态码 */
    private final HttpStatus httpStatus;

    ErrorCode(int code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }
}
