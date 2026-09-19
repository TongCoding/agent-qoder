package com.example.taskhub.dto.todo;

import com.example.taskhub.common.PageQuery;
import com.example.taskhub.enums.TodoPriority;
import com.example.taskhub.enums.TodoStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * 待办事项分页查询条件。
 *
 * <p>所有字段均为可选，为空时不参与过滤；多个条件之间为 AND 关系。</p>
 *
 * @author TaskHub
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "待办事项分页查询条件")
public class TodoQueryRequest extends PageQuery {

    /** 关键字：模糊匹配标题与描述 */
    @Schema(description = "关键字，模糊匹配标题 / 描述", example = "周报")
    private String keyword;

    /** 按状态筛选 */
    @Schema(description = "按状态筛选", example = "IN_PROGRESS")
    private TodoStatus status;

    /** 按优先级筛选 */
    @Schema(description = "按优先级筛选", example = "HIGH")
    private TodoPriority priority;

    /** 按负责人 ID 筛选 */
    @Schema(description = "按负责人用户 ID 筛选", example = "1")
    private Long assigneeId;

    /** 仅看未分配负责人的待办 */
    @Schema(description = "是否只看未分配负责人的待办", example = "false")
    private Boolean unassigned;

    /** 只看逾期（true）或未逾期（false），为空则不过滤 */
    @Schema(description = "只看逾期：true 逾期 / false 未逾期，为空不过滤", example = "true")
    private Boolean overdue;

    /** 截止日期区间起点（含） */
    @Schema(description = "截止日期区间起点（含），格式 yyyy-MM-dd", example = "2026-01-01")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dueDateFrom;

    /** 截止日期区间终点（含） */
    @Schema(description = "截止日期区间终点（含），格式 yyyy-MM-dd", example = "2026-12-31")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dueDateTo;

    /** 待办列表允许排序的字段白名单 */
    public static final String[] SORTABLE_FIELDS = {
            "id", "title", "status", "priority", "dueDate", "completedAt", "createdAt", "updatedAt"
    };

    /** 默认排序字段 */
    public static final String DEFAULT_SORT_FIELD = "createdAt";
}
