package com.example.taskhub.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 用户精简信息，用于下拉选择器等场景（如为待办指派负责人）。
 *
 * @author TaskHub
 */
@Schema(description = "用户下拉选项")
public record UserOptionResponse(

        @Schema(description = "用户 ID", example = "1")
        Long id,

        @Schema(description = "登录用户名", example = "zhangsan")
        String username,

        @Schema(description = "昵称", example = "张三")
        String nickname,

        @Schema(description = "所属部门", example = "研发部")
        String department
) {
}
