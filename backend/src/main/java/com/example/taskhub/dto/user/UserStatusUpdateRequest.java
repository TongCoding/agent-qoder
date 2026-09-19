package com.example.taskhub.dto.user;

import com.example.taskhub.enums.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * 更新用户账号状态请求体（对应 PATCH 接口，只修改单个字段）。
 *
 * @author TaskHub
 */
@Schema(description = "更新用户状态请求")
public record UserStatusUpdateRequest(

        @Schema(description = "目标状态", example = "DISABLED", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "账号状态不能为空")
        UserStatus status
) {
}
