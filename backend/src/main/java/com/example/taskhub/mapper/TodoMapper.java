package com.example.taskhub.mapper;

import com.example.taskhub.dto.todo.TodoCreateRequest;
import com.example.taskhub.dto.todo.TodoResponse;
import com.example.taskhub.dto.todo.TodoUpdateRequest;
import com.example.taskhub.entity.TodoItem;
import com.example.taskhub.entity.User;
import com.example.taskhub.enums.TodoPriority;
import com.example.taskhub.enums.TodoStatus;

import java.time.LocalDateTime;

/**
 * 待办事项实体与 DTO 之间的手动转换器。
 *
 * @author TaskHub
 */
public final class TodoMapper {

    private TodoMapper() {
        // 工具类，禁止实例化
    }

    /**
     * 创建请求 -> 实体。
     *
     * <p>状态与优先级为空时回退到默认值（PENDING / MEDIUM）。</p>
     *
     * @param request  创建请求
     * @param assignee 负责人实体，可为 null
     */
    public static TodoItem toEntity(TodoCreateRequest request, User assignee) {
        TodoStatus status = request.status() == null ? TodoStatus.PENDING : request.status();
        return TodoItem.builder()
                .title(request.title().trim())
                .description(emptyToNull(request.description()))
                .status(status)
                .priority(request.priority() == null ? TodoPriority.MEDIUM : request.priority())
                .dueDate(request.dueDate())
                // 直接创建时若状态为 DONE，需要同步写入完成时间
                .completedAt(status == TodoStatus.DONE ? LocalDateTime.now() : null)
                .assignee(assignee)
                .build();
    }

    /**
     * 将更新请求的字段写回已存在的实体。
     *
     * <p>状态变化统一走 {@link TodoItem#changeStatus(TodoStatus)}，
     * 以保证 {@code completedAt} 与状态始终一致。</p>
     *
     * @param target   数据库中已加载的实体
     * @param request  更新请求
     * @param assignee 新的负责人实体，可为 null
     */
    public static void applyUpdate(TodoItem target, TodoUpdateRequest request, User assignee) {
        target.setTitle(request.title().trim());
        target.setDescription(emptyToNull(request.description()));
        target.setPriority(request.priority());
        target.setDueDate(request.dueDate());
        target.setAssignee(assignee);
        target.changeStatus(request.status());
    }

    /**
     * 实体 -> 响应 DTO。
     *
     * <p>访问 {@code assignee} 会触发懒加载，因此必须在事务内调用（Service 层已保证）。</p>
     */
    public static TodoResponse toResponse(TodoItem item) {
        User assignee = item.getAssignee();
        TodoResponse.Assignee assigneeDto = assignee == null
                ? null
                : new TodoResponse.Assignee(assignee.getId(), assignee.getUsername(), assignee.getNickname());

        return new TodoResponse(
                item.getId(),
                item.getTitle(),
                item.getDescription(),
                item.getStatus(),
                item.getPriority(),
                item.getDueDate(),
                item.getCompletedAt(),
                item.isOverdue(),
                assigneeDto,
                item.getCreatedAt(),
                item.getUpdatedAt()
        );
    }

    private static String emptyToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}
