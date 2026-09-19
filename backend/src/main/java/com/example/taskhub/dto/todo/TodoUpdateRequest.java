package com.example.taskhub.dto.todo;

import com.example.taskhub.enums.TodoPriority;
import com.example.taskhub.enums.TodoStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * 更新待办事项请求体（全量更新语义）。
 *
 * <p>与创建请求的差异：{@code status} 与 {@code priority} 为必填，
 * 避免编辑表单未回传这两个字段时被静默重置为默认值。</p>
 *
 * @author TaskHub
 */
@Schema(description = "更新待办事项请求")
public record TodoUpdateRequest(

        @Schema(description = "标题，必填", example = "编写项目周报（修订版）", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "标题不能为空")
        @Size(max = 120, message = "标题长度不能超过 120 个字符")
        String title,

        @Schema(description = "详细描述", example = "补充本周风险项与应对措施")
        @Size(max = 1000, message = "描述长度不能超过 1000 个字符")
        String description,

        @Schema(description = "状态", example = "IN_PROGRESS", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "状态不能为空")
        TodoStatus status,

        @Schema(description = "优先级", example = "HIGH", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "优先级不能为空")
        TodoPriority priority,

        @Schema(description = "截止日期（yyyy-MM-dd），传 null 表示清除", example = "2026-12-31")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate dueDate,

        @Schema(description = "负责人用户 ID，传 null 表示取消分配", example = "1")
        Long assigneeId
) {
}
