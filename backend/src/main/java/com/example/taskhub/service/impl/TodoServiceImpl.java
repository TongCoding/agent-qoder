package com.example.taskhub.service.impl;

import com.example.taskhub.common.ErrorCode;
import com.example.taskhub.common.PageResult;
import com.example.taskhub.dto.todo.TodoCreateRequest;
import com.example.taskhub.dto.todo.TodoQueryRequest;
import com.example.taskhub.dto.todo.TodoResponse;
import com.example.taskhub.dto.todo.TodoStatisticsResponse;
import com.example.taskhub.dto.todo.TodoStatusUpdateRequest;
import com.example.taskhub.dto.todo.TodoUpdateRequest;
import com.example.taskhub.entity.TodoItem;
import com.example.taskhub.entity.User;
import com.example.taskhub.enums.TodoPriority;
import com.example.taskhub.enums.TodoStatus;
import com.example.taskhub.exception.BusinessException;
import com.example.taskhub.mapper.TodoMapper;
import com.example.taskhub.repository.TodoRepository;
import com.example.taskhub.repository.UserRepository;
import com.example.taskhub.repository.specification.TodoSpecifications;
import com.example.taskhub.service.TodoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 待办事项服务实现。
 *
 * @author TaskHub
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TodoServiceImpl implements TodoService {

    /** 仪表盘"最新动态"展示的条数 */
    private static final int RECENT_TODO_LIMIT = 5;

    /** 未完成状态集合，用于逾期与今日到期统计 */
    private static final List<TodoStatus> UNFINISHED_STATUSES = Arrays.stream(TodoStatus.values())
            .filter(status -> !status.isFinished())
            .toList();

    private final TodoRepository todoRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public TodoResponse create(TodoCreateRequest request) {
        User assignee = resolveAssignee(request.assigneeId());

        TodoItem saved = todoRepository.save(TodoMapper.toEntity(request, assignee));
        log.info("创建待办成功 | id={} | title={}", saved.getId(), saved.getTitle());
        return TodoMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public TodoResponse update(Long id, TodoUpdateRequest request) {
        TodoItem item = getExistingTodo(id);
        User assignee = resolveAssignee(request.assigneeId());

        TodoMapper.applyUpdate(item, request, assignee);
        TodoItem saved = todoRepository.save(item);
        log.info("更新待办成功 | id={} | status={}", saved.getId(), saved.getStatus());
        return TodoMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public TodoResponse changeStatus(Long id, TodoStatusUpdateRequest request) {
        TodoItem item = getExistingTodo(id);

        if (item.getStatus() == request.status()) {
            return TodoMapper.toResponse(item);
        }
        item.changeStatus(request.status());

        TodoItem saved = todoRepository.save(item);
        log.info("待办状态流转 | id={} | status={}", id, saved.getStatus());
        return TodoMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public TodoResponse getById(Long id) {
        // 使用 join fetch 一次性取出负责人，避免二次查询
        TodoItem item = todoRepository.findByIdWithAssignee(id)
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.TODO_NOT_FOUND, id));
        return TodoMapper.toResponse(item);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<TodoResponse> page(TodoQueryRequest query) {
        TodoQueryRequest condition = query == null ? new TodoQueryRequest() : query;
        Pageable pageable = condition.toPageable(
                TodoQueryRequest.DEFAULT_SORT_FIELD,
                TodoQueryRequest.SORTABLE_FIELDS
        );

        Page<TodoItem> page = todoRepository.findAll(TodoSpecifications.filter(condition), pageable);
        // Specification 中已 fetch 负责人，此处转换不会触发 N+1
        return PageResult.from(page, TodoMapper::toResponse);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        TodoItem item = getExistingTodo(id);
        todoRepository.delete(item);
        log.info("删除待办成功 | id={} | title={}", id, item.getTitle());
    }

    @Override
    @Transactional
    public int deleteBatch(List<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "待删除的 ID 列表不能为空");
        }

        // 去重，避免前端多选时重复 ID 造成的误判
        Set<Long> distinctIds = new HashSet<>(ids);
        List<TodoItem> items = todoRepository.findAllById(distinctIds);

        if (items.size() != distinctIds.size()) {
            Set<Long> foundIds = items.stream().map(TodoItem::getId).collect(Collectors.toSet());
            List<Long> missing = distinctIds.stream().filter(id -> !foundIds.contains(id)).sorted().toList();
            throw new BusinessException(ErrorCode.TODO_NOT_FOUND,
                    "以下待办不存在或已被删除：" + missing);
        }

        todoRepository.deleteAll(items);
        log.info("批量删除待办成功 | count={} | ids={}", items.size(), distinctIds);
        return items.size();
    }

    @Override
    @Transactional(readOnly = true)
    public TodoStatisticsResponse statistics() {
        long total = todoRepository.count();

        Map<TodoStatus, Long> statusCounts = new EnumMap<>(TodoStatus.class);
        for (TodoStatus status : TodoStatus.values()) {
            statusCounts.put(status, 0L);
        }
        todoRepository.countGroupByStatus().forEach(item -> statusCounts.put(item.getStatus(), item.getTotal()));

        Map<TodoPriority, Long> priorityCounts = new EnumMap<>(TodoPriority.class);
        for (TodoPriority priority : TodoPriority.values()) {
            priorityCounts.put(priority, 0L);
        }
        todoRepository.countGroupByPriority().forEach(item -> priorityCounts.put(item.getPriority(), item.getTotal()));

        LocalDate today = LocalDate.now();
        long overdueCount = todoRepository.countByDueDateBeforeAndStatusIn(today, UNFINISHED_STATUSES);
        long dueTodayCount = todoRepository.countByDueDateAndStatusIn(today, UNFINISHED_STATUSES);
        long unassignedCount = todoRepository.countByAssigneeIsNull();

        List<TodoResponse> recentTodos = todoRepository
                .findRecentWithAssignee(PageRequest.of(0, RECENT_TODO_LIMIT))
                .stream()
                .map(TodoMapper::toResponse)
                .toList();

        return new TodoStatisticsResponse(
                total,
                statusCounts,
                priorityCounts,
                overdueCount,
                dueTodayCount,
                unassignedCount,
                completionRate(total, statusCounts.getOrDefault(TodoStatus.DONE, 0L)),
                recentTodos
        );
    }

    // ==================================================================
    // 内部工具方法
    // ==================================================================

    /**
     * 按 ID 加载待办，不存在时抛出业务异常。
     */
    private TodoItem getExistingTodo(Long id) {
        if (id == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "待办 ID 不能为空");
        }
        return todoRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.TODO_NOT_FOUND, id));
    }

    /**
     * 解析负责人：ID 为空表示不分配，否则必须存在对应用户。
     *
     * @param assigneeId 负责人 ID，可为 null
     * @return 用户实体或 null
     */
    private User resolveAssignee(Long assigneeId) {
        if (assigneeId == null) {
            return null;
        }
        return userRepository.findById(assigneeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND,
                        "指定的负责人不存在，ID = " + assigneeId));
    }

    /**
     * 计算完成率，保留两位小数。
     *
     * @param total    总数
     * @param doneCount 已完成数
     * @return 0-100 之间的百分比
     */
    private double completionRate(long total, long doneCount) {
        if (total <= 0) {
            return 0.0D;
        }
        return Math.round(doneCount * 10000.0D / total) / 100.0D;
    }
}
