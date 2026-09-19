package com.example.taskhub.dto.todo;

import com.example.taskhub.enums.TodoPriority;
import com.example.taskhub.enums.TodoStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

/**
 * 待办事项统计结果，用于前端仪表盘展示。
 *
 * <p>一次请求返回全部聚合指标，避免仪表盘发起多次统计调用。</p>
 *
 * @param total          待办总数
 * @param statusCounts   各状态数量，key 为状态枚举名
 * @param priorityCounts 各优先级数量，key 为优先级枚举名
 * @param overdueCount   已逾期数量（未完成且截止日期早于今天）
 * @param dueTodayCount  今日到期数量
 * @param unassignedCount 未分配负责人数量
 * @param completionRate 完成率（0-100，保留两位小数）
 * @param recentTodos    最近更新的若干条待办，便于仪表盘直接渲染列表
 * @author TaskHub
 */
@Schema(description = "待办事项统计")
public record TodoStatisticsResponse(

        @Schema(description = "待办总数", example = "42")
        long total,

        @Schema(description = "各状态数量")
        Map<TodoStatus, Long> statusCounts,

        @Schema(description = "各优先级数量")
        Map<TodoPriority, Long> priorityCounts,

        @Schema(description = "已逾期数量", example = "3")
        long overdueCount,

        @Schema(description = "今日到期数量", example = "2")
        long dueTodayCount,

        @Schema(description = "未分配负责人数量", example = "5")
        long unassignedCount,

        @Schema(description = "完成率（百分比，0-100）", example = "45.24")
        double completionRate,

        @Schema(description = "最近更新的待办列表")
        List<TodoResponse> recentTodos
) {
}
