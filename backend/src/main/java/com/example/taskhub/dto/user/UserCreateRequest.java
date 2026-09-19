package com.example.taskhub.dto.user;

import com.example.taskhub.enums.UserRole;
import com.example.taskhub.enums.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 创建用户请求体。
 *
 * @author TaskHub
 */
@Schema(description = "创建用户请求")
public record UserCreateRequest(

        @Schema(description = "登录用户名，全局唯一，4-50 位字母/数字/下划线", example = "zhangsan", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "用户名不能为空")
        @Size(min = 4, max = 50, message = "用户名长度必须在 4-50 个字符之间")
        @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "用户名只能包含字母、数字和下划线")
        String username,

        @Schema(description = "昵称 / 显示名", example = "张三")
        @Size(max = 50, message = "昵称长度不能超过 50 个字符")
        String nickname,

        @Schema(description = "邮箱，全局唯一", example = "zhangsan@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "邮箱不能为空")
        @Email(message = "邮箱格式不正确")
        @Size(max = 100, message = "邮箱长度不能超过 100 个字符")
        String email,

        @Schema(description = "手机号", example = "13800138000")
        @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
        String phone,

        @Schema(description = "所属部门", example = "研发部")
        @Size(max = 50, message = "部门名称长度不能超过 50 个字符")
        String department,

        @Schema(description = "角色", example = "MEMBER", defaultValue = "MEMBER")
        @NotNull(message = "角色不能为空")
        UserRole role,

        @Schema(description = "账号状态", example = "ACTIVE", defaultValue = "ACTIVE")
        @NotNull(message = "账号状态不能为空")
        UserStatus status,

        @Schema(description = "备注", example = "新入职员工")
        @Size(max = 255, message = "备注长度不能超过 255 个字符")
        String remark
) {
}
