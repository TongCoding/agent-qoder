package com.example.taskhub.entity;

import com.example.taskhub.enums.TodoPriority;
import com.example.taskhub.enums.TodoStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 待办事项实体。
 *
 * <p>与 {@link User} 为多对一关系：一个用户可以拥有多个待办，一个待办最多指派给一个负责人。
 * 关联采用 {@code LAZY} 加载，避免列表查询时的 N+1 放大；如需一次性取出负责人，
 * Repository 中提供了 {@code JOIN FETCH} 的查询方法。</p>
 *
 * @author TaskHub
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "todo_item",
        indexes = {
                @Index(name = "idx_todo_status", columnList = "status"),
                @Index(name = "idx_todo_priority", columnList = "priority"),
                @Index(name = "idx_todo_due_date", columnList = "due_date"),
                @Index(name = "idx_todo_assignee", columnList = "assignee_id")
        }
)
public class TodoItem extends BaseEntity {

    /** 标题，必填 */
    @Column(name = "title", nullable = false, length = 120)
    private String title;

    /** 详细描述 */
    @Column(name = "description", length = 1000)
    private String description;

    /** 状态 */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private TodoStatus status = TodoStatus.PENDING;

    /** 优先级 */
    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 20)
    @Builder.Default
    private TodoPriority priority = TodoPriority.MEDIUM;

    /** 截止日期 */
    @Column(name = "due_date")
    private LocalDate dueDate;

    /** 实际完成时间，仅在流转到 DONE 时写入 */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    /** 负责人（可为空，表示尚未分配） */
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(
            name = "assignee_id",
            foreignKey = @ForeignKey(name = "fk_todo_assignee")
    )
    private User assignee;

    // ==================== 领域行为 ====================

    /**
     * 流转任务状态，并维护 {@code completedAt} 字段。
     *
     * <p>把状态相关的副作用收敛在实体内部，避免 Service 层散落重复逻辑。</p>
     *
     * @param target 目标状态
     */
    public void changeStatus(TodoStatus target) {
        this.status = target;
        this.completedAt = (target == TodoStatus.DONE) ? LocalDateTime.now() : null;
    }

    /**
     * 是否已逾期：存在截止日期、尚未完成，且截止日期早于今天。
     */
    public boolean isOverdue() {
        return dueDate != null
                && !status.isFinished()
                && dueDate.isBefore(LocalDate.now());
    }

    /**
     * 是否已指派负责人。
     */
    public boolean isAssigned() {
        return assignee != null;
    }
}
