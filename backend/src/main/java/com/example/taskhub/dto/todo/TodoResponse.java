package com.example.taskhub.dto.todo;

import com.example.taskhub.enums.TodoPriority;
import com.example.taskhub.enums.TodoStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 待办事项响应体。
 *
 * <p>{@code overdue} 为服务端计算得出的派生字段，前端无需重复实现逾期判断逻辑。</p>
 *
 * @author TaskHub
 */
@Schema(description = "待办事项详情")
public record TodoResponse(

        @Schema(description = "待办 ID", example = "1")
        Long id,

        @Schema(description = "标题", example = "编写项目周报")
        String title,

        @Schema(description = "详细描述", example = "汇总本周研发进度、风险与下周计划")
        String description,

        @Schema(description = "状态", example = "IN_PROGRESS")
        TodoStatus status,

        @Schema(description = "优先级", example = "HIGH")
        TodoPriority priority,

        @Schema(description = "截止日期", example = "2026-12-31")
        LocalDate dueDate,

        @Schema(description = "实际完成时间，未完成时为 null", example = "2026-12-30T18:20:00")
        LocalDateTime completedAt,

        @Schema(description = "是否已逾期", example = "false")
        boolean overdue,

        @Schema(description = "负责人信息，未分配时为 null")
        Assignee assignee,

        @Schema(description = "创建时间", example = "2026-01-01T10:00:00")
        LocalDateTime createdAt,

        @Schema(description = "最后更新时间", example = "2026-01-02T10:00:00")
        LocalDateTime updatedAt
) {

    /**
     * 负责人精简信息，内嵌在待办详情中返回，避免前端二次请求用户接口。
     *
     * @param id       用户 ID
     * @param username 登录用户名
     * @param nickname 昵称
     */
    @Schema(description = "待办负责人精简信息")
    public record Assignee(
            @Schema(description = "用户 ID", example = "1") Long id,
            @Schema(description = "登录用户名", example = "zhangsan") String username,
            @Schema(description = "昵称", example = "张三") String nickname
    ) {
    }
}
