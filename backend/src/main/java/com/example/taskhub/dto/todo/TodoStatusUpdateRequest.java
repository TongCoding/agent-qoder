package com.example.taskhub.dto.todo;

import com.example.taskhub.enums.TodoStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * 更新待办状态请求体（对应 PATCH 接口）。
 *
 * @author TaskHub
 */
@Schema(description = "更新待办状态请求")
public record TodoStatusUpdateRequest(

        @Schema(description = "目标状态", example = "DONE", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "状态不能为空")
        TodoStatus status
) {
}
