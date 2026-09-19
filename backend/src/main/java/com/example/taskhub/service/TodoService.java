package com.example.taskhub.service;

import com.example.taskhub.common.PageResult;
import com.example.taskhub.dto.todo.TodoCreateRequest;
import com.example.taskhub.dto.todo.TodoQueryRequest;
import com.example.taskhub.dto.todo.TodoResponse;
import com.example.taskhub.dto.todo.TodoStatisticsResponse;
import com.example.taskhub.dto.todo.TodoStatusUpdateRequest;
import com.example.taskhub.dto.todo.TodoUpdateRequest;

import java.util.List;

/**
 * 待办事项服务接口。
 *
 * @author TaskHub
 */
public interface TodoService {

    /**
     * 创建待办事项。
     *
     * @param request 创建请求
     * @return 创建后的待办详情
     * @throws com.example.taskhub.exception.BusinessException 指定的负责人不存在时抛出
     */
    TodoResponse create(TodoCreateRequest request);

    /**
     * 全量更新待办事项。
     *
     * @param id      待办 ID
     * @param request 更新请求
     * @return 更新后的待办详情
     * @throws com.example.taskhub.exception.BusinessException 待办或负责人不存在时抛出
     */
    TodoResponse update(Long id, TodoUpdateRequest request);

    /**
     * 仅流转待办状态，同时维护完成时间。
     *
     * @param id      待办 ID
     * @param request 状态更新请求
     * @return 更新后的待办详情
     */
    TodoResponse changeStatus(Long id, TodoStatusUpdateRequest request);

    /**
     * 查询待办详情。
     *
     * @param id 待办 ID
     * @return 待办详情
     * @throws com.example.taskhub.exception.BusinessException 待办不存在时抛出
     */
    TodoResponse getById(Long id);

    /**
     * 按条件分页查询待办。
     *
     * @param query 查询条件
     * @return 分页结果
     */
    PageResult<TodoResponse> page(TodoQueryRequest query);

    /**
     * 删除单条待办。
     *
     * @param id 待办 ID
     * @throws com.example.taskhub.exception.BusinessException 待办不存在时抛出
     */
    void delete(Long id);

    /**
     * 批量删除待办。
     *
     * <p>采用「先查询再删除」而非直接 {@code deleteAllById}，
     * 目的是在存在无效 ID 时给出明确提示，而不是静默忽略。</p>
     *
     * @param ids 待办 ID 列表
     * @return 实际删除的条数
     * @throws com.example.taskhub.exception.BusinessException 列表为空或包含不存在的 ID 时抛出
     */
    int deleteBatch(List<Long> ids);

    /**
     * 统计待办分布情况，用于仪表盘。
     *
     * @return 统计结果（含最近更新的 5 条待办）
     */
    TodoStatisticsResponse statistics();
}
