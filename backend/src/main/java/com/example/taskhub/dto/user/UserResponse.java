package com.example.taskhub.dto.user;

import com.example.taskhub.enums.UserRole;
import com.example.taskhub.enums.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 用户信息响应体。
 *
 * @param todoCount 该用户名下（作为负责人）的待办总数，由 Service 层批量统计后填充，避免 N+1 查询
 * @author TaskHub
 */
@Schema(description = "用户信息")
public record UserResponse(

        @Schema(description = "用户 ID", example = "1")
        Long id,

        @Schema(description = "登录用户名", example = "zhangsan")
        String username,

        @Schema(description = "昵称", example = "张三")
        String nickname,

        @Schema(description = "邮箱", example = "zhangsan@example.com")
        String email,

        @Schema(description = "手机号", example = "13800138000")
        String phone,

        @Schema(description = "所属部门", example = "研发部")
        String department,

        @Schema(description = "角色", example = "MEMBER")
        UserRole role,

        @Schema(description = "账号状态", example = "ACTIVE")
        UserStatus status,

        @Schema(description = "备注", example = "新入职员工")
        String remark,

        @Schema(description = "名下待办事项数量", example = "5")
        long todoCount,

        @Schema(description = "创建时间", example = "2026-01-01T10:00:00")
        LocalDateTime createdAt,

        @Schema(description = "最后更新时间", example = "2026-01-02T10:00:00")
        LocalDateTime updatedAt
) {
}
