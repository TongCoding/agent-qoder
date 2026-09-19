package com.example.taskhub.exception;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 字段级校验错误明细。
 *
 * <p>当请求参数校验失败时，{@code ApiResponse.data} 会返回该结构的列表，
 * 前端可据此把错误信息精确地渲染到对应表单项下方。</p>
 *
 * @param field         出错的字段名（请求体字段或查询参数名）
 * @param message       错误提示
 * @param rejectedValue 被拒绝的原始值
 * @author TaskHub
 */
@Schema(description = "字段校验错误明细")
public record FieldValidationError(
        @Schema(description = "出错字段名", example = "title") String field,
        @Schema(description = "错误提示", example = "标题不能为空") String message,
        @Schema(description = "被拒绝的原始值", example = "") Object rejectedValue
) {
}
