package com.example.taskhub.controller;

import com.example.taskhub.common.ApiResponse;
import com.example.taskhub.common.PageResult;
import com.example.taskhub.dto.todo.TodoCreateRequest;
import com.example.taskhub.dto.todo.TodoQueryRequest;
import com.example.taskhub.dto.todo.TodoResponse;
import com.example.taskhub.dto.todo.TodoStatisticsResponse;
import com.example.taskhub.dto.todo.TodoStatusUpdateRequest;
import com.example.taskhub.dto.todo.TodoUpdateRequest;
import com.example.taskhub.service.TodoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 待办事项 REST 接口。
 *
 * <p>基础路径：{@code /api/v1/todos}</p>
 *
 * @author TaskHub
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/todos")
@Tag(name = "01. 待办事项", description = "待办事项的增删改查、状态流转与统计")
public class TodoController {

    private final TodoService todoService;

    /**
     * 分页查询待办列表。
     */
    @GetMapping
    @Operation(summary = "分页查询待办列表",
            description = "支持关键字（标题/描述）、状态、优先级、负责人、逾期与截止日期区间组合筛选；"
                    + "page 从 1 开始，sortBy 仅接受白名单字段，非法值会回退为默认排序。"
                    + "失败时统一返回 `{ code, message, data }` 结构，例如参数校验失败 code = 40001。")
    public ApiResponse<PageResult<TodoResponse>> page(@Valid @ParameterObject TodoQueryRequest query) {
        log.debug("分页查询待办 | {}", query);
        return ApiResponse.success(todoService.page(query));
    }

    /**
     * 查询待办详情。
     */
    @GetMapping("/{id}")
    @Operation(summary = "查询待办详情", description = "返回单条待办的完整信息，包含负责人精简信息")
    public ApiResponse<TodoResponse> getById(
            @Parameter(description = "待办 ID", example = "1", required = true) @PathVariable Long id) {
        return ApiResponse.success(todoService.getById(id));
    }

    /**
     * 查询待办统计数据（仪表盘）。
     *
     * <p>注意：该映射必须声明在 {@code /{id}} 之前或保持字面量路径，
     * Spring 会优先匹配更具体的字面量路径，不会被 {@code /{id}} 拦截。</p>
     */
    @GetMapping("/statistics")
    @Operation(summary = "查询待办统计", description = "返回总数、各状态/优先级分布、逾期数、今日到期数、未分配数、完成率与最近更新的 5 条待办")
    public ApiResponse<TodoStatisticsResponse> statistics() {
        return ApiResponse.success(todoService.statistics());
    }

    /**
     * 创建待办。
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "创建待办", description = "status 与 priority 可不传，服务端默认 PENDING / MEDIUM")
    public ApiResponse<TodoResponse> create(@Valid @RequestBody TodoCreateRequest request) {
        TodoResponse created = todoService.create(request);
        return ApiResponse.success("创建成功", created);
    }

    /**
     * 全量更新待办。
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新待办", description = "全量更新语义，请求体需包含所有可编辑字段")
    public ApiResponse<TodoResponse> update(
            @Parameter(description = "待办 ID", example = "1", required = true) @PathVariable Long id,
            @Valid @RequestBody TodoUpdateRequest request) {
        return ApiResponse.success("更新成功", todoService.update(id, request));
    }

    /**
     * 仅流转待办状态。
     */
    @PatchMapping("/{id}/status")
    @Operation(summary = "变更待办状态",
            description = "PATCH 语义，只修改 status 字段；流转为 DONE 时服务端会自动写入 completedAt")
    public ApiResponse<TodoResponse> changeStatus(
            @Parameter(description = "待办 ID", example = "1", required = true) @PathVariable Long id,
            @Valid @RequestBody TodoStatusUpdateRequest request) {
        return ApiResponse.success("状态已更新", todoService.changeStatus(id, request));
    }

    /**
     * 删除单条待办。
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除待办")
    public ApiResponse<Void> delete(
            @Parameter(description = "待办 ID", example = "1", required = true) @PathVariable Long id) {
        todoService.delete(id);
        return ApiResponse.success("删除成功", null);
    }

    /**
     * 批量删除待办。
     *
     * <p>使用查询参数而非请求体传递 ID 列表，因为 DELETE 携带 body 在部分代理与客户端中支持不佳。</p>
     */
    @DeleteMapping
    @Operation(summary = "批量删除待办", description = "通过 ids 查询参数传入待删除的 ID 列表；若存在无效 ID 则整体失败，不做部分删除")
    public ApiResponse<Integer> deleteBatch(
            @Parameter(description = "待办 ID 列表", example = "1,2,3", required = true)
            @RequestParam List<Long> ids) {
        int deleted = todoService.deleteBatch(ids);
        return ApiResponse.success("已删除 " + deleted + " 条待办", deleted);
    }
}
