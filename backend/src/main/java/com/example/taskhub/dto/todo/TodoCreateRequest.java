package com.example.taskhub.dto.todo;

import com.example.taskhub.enums.TodoPriority;
import com.example.taskhub.enums.TodoStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * 创建待办事项请求体。
 *
 * <p>{@code status} 与 {@code priority} 允许为空，服务端会分别回退为
 * {@code PENDING} 与 {@code MEDIUM}。</p>
 *
 * @author TaskHub
 */
@Schema(description = "创建待办事项请求")
public record TodoCreateRequest(

        @Schema(description = "标题，必填", example = "编写项目周报", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "标题不能为空")
        @Size(max = 120, message = "标题长度不能超过 120 个字符")
        String title,

        @Schema(description = "详细描述", example = "汇总本周研发进度、风险与下周计划")
        @Size(max = 1000, message = "描述长度不能超过 1000 个字符")
        String description,

        @Schema(description = "状态，为空时默认 PENDING", example = "PENDING")
        TodoStatus status,

        @Schema(description = "优先级，为空时默认 MEDIUM", example = "HIGH")
        TodoPriority priority,

        @Schema(description = "截止日期（yyyy-MM-dd），允许早于今天以支持补录历史任务", example = "2026-12-31")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate dueDate,

        @Schema(description = "负责人用户 ID，为空表示暂不分配", example = "1")
        Long assigneeId
) {
}
