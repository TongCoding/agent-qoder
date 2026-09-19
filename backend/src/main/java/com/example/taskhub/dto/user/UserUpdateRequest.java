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
 * 更新用户请求体（全量更新语义）。
 *
 * <p>与创建请求的差异：{@code username} 一旦创建即作为业务主键使用，为避免关联数据错乱，
 * 这里不允许修改，因此不包含该字段。</p>
 *
 * @author TaskHub
 */
@Schema(description = "更新用户请求")
public record UserUpdateRequest(

        @Schema(description = "昵称 / 显示名", example = "张三丰")
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

        @Schema(description = "所属部门", example = "架构组")
        @Size(max = 50, message = "部门名称长度不能超过 50 个字符")
        String department,

        @Schema(description = "角色", example = "MANAGER", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "角色不能为空")
        UserRole role,

        @Schema(description = "账号状态", example = "ACTIVE", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "账号状态不能为空")
        UserStatus status,

        @Schema(description = "备注", example = "已晋升为项目经理")
        @Size(max = 255, message = "备注长度不能超过 255 个字符")
        String remark
) {
}
