package com.example.taskhub.service.impl;

import com.example.taskhub.common.ErrorCode;
import com.example.taskhub.dto.todo.TodoCreateRequest;
import com.example.taskhub.dto.todo.TodoResponse;
import com.example.taskhub.dto.todo.TodoStatisticsResponse;
import com.example.taskhub.dto.todo.TodoStatusUpdateRequest;
import com.example.taskhub.entity.TodoItem;
import com.example.taskhub.entity.User;
import com.example.taskhub.enums.TodoPriority;
import com.example.taskhub.enums.TodoStatus;
import com.example.taskhub.enums.UserRole;
import com.example.taskhub.enums.UserStatus;
import com.example.taskhub.exception.BusinessException;
import com.example.taskhub.repository.TodoRepository;
import com.example.taskhub.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

/**
 * 待办服务单元测试。
 *
 * <p>与集成测试的分工：这里用 Mockito 隔离数据库，专注验证 Service 内部的
 * <b>分支逻辑与领域规则</b>（默认值回退、状态流转副作用、完成率计算、异常抛出），
 * 执行速度快且不依赖 H2。</p>
 *
 * @author TaskHub
 */
@ExtendWith(MockitoExtension.class)
class TodoServiceImplTest {

    @Mock
    private TodoRepository todoRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TodoServiceImpl todoService;

    @Test
    @DisplayName("创建：status 与 priority 为空时回退为 PENDING / MEDIUM")
    void shouldFallbackToDefaultStatusAndPriority() {
        given(userRepository.findById(1L)).willReturn(Optional.of(buildUser()));
        given(todoRepository.save(any(TodoItem.class))).willAnswer(invocation -> {
            TodoItem item = invocation.getArgument(0);
            item.setId(100L);
            return item;
        });

        TodoCreateRequest request = new TodoCreateRequest(
                "新任务", null, null, null, null, 1L);

        TodoResponse response = todoService.create(request);

        ArgumentCaptor<TodoItem> captor = ArgumentCaptor.forClass(TodoItem.class);
        then(todoRepository).should().save(captor.capture());

        TodoItem persisted = captor.getValue();
        assertThat(persisted.getStatus()).isEqualTo(TodoStatus.PENDING);
        assertThat(persisted.getPriority()).isEqualTo(TodoPriority.MEDIUM);
        assertThat(persisted.getAssignee().getId()).isEqualTo(1L);
        assertThat(persisted.getCompletedAt()).isNull();

        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.title()).isEqualTo("新任务");
        assertThat(response.status()).isEqualTo(TodoStatus.PENDING);
        assertThat(response.assignee()).isNotNull();
        assertThat(response.assignee().username()).isEqualTo("tester");
    }

    @Test
    @DisplayName("创建：直接以 DONE 状态创建时应写入完成时间")
    void shouldFillCompletedAtWhenCreatedAsDone() {
        given(todoRepository.save(any(TodoItem.class))).willAnswer(invocation -> invocation.getArgument(0));

        todoService.create(new TodoCreateRequest(
                "已完成的任务", null, TodoStatus.DONE, TodoPriority.HIGH, null, null));

        ArgumentCaptor<TodoItem> captor = ArgumentCaptor.forClass(TodoItem.class);
        then(todoRepository).should().save(captor.capture());
        assertThat(captor.getValue().getCompletedAt()).isNotNull();
        // 未分配负责人时不应查询用户表
        then(userRepository).should(never()).findById(any());
    }

    @Test
    @DisplayName("创建：负责人不存在时抛出 USER_NOT_FOUND")
    void shouldThrowWhenAssigneeMissing() {
        given(userRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> todoService.create(new TodoCreateRequest(
                "任务", null, null, null, null, 999L)))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.USER_NOT_FOUND);

        then(todoRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("状态流转：改为 DONE 时写入 completedAt，改为其他状态时清空")
    void shouldMaintainCompletedAtOnStatusChange() {
        TodoItem item = TodoItem.builder()
                .title("任务")
                .status(TodoStatus.IN_PROGRESS)
                .priority(TodoPriority.MEDIUM)
                .build();
        item.setId(1L);
        item.changeStatus(TodoStatus.DONE);

        given(todoRepository.findById(1L)).willReturn(Optional.of(item));
        given(todoRepository.save(any(TodoItem.class))).willAnswer(invocation -> invocation.getArgument(0));

        TodoResponse done = todoService.changeStatus(1L, new TodoStatusUpdateRequest(TodoStatus.DONE));
        assertThat(done.completedAt()).isNotNull();
        assertThat(done.overdue()).isFalse();

        TodoResponse reopened = todoService.changeStatus(1L, new TodoStatusUpdateRequest(TodoStatus.PENDING));
        assertThat(reopened.status()).isEqualTo(TodoStatus.PENDING);
        assertThat(reopened.completedAt()).isNull();
    }

    @Test
    @DisplayName("状态流转：目标状态与当前一致时不触发保存")
    void shouldSkipSaveWhenStatusUnchanged() {
        TodoItem item = TodoItem.builder()
                .title("任务")
                .status(TodoStatus.PENDING)
                .priority(TodoPriority.MEDIUM)
                .build();
        item.setId(1L);
        given(todoRepository.findById(1L)).willReturn(Optional.of(item));

        TodoResponse response = todoService.changeStatus(1L, new TodoStatusUpdateRequest(TodoStatus.PENDING));

        assertThat(response.status()).isEqualTo(TodoStatus.PENDING);
        then(todoRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("查询：待办不存在时抛出 TODO_NOT_FOUND 且携带 ID 信息")
    void shouldThrowWhenTodoNotFound() {
        given(todoRepository.findByIdWithAssignee(404L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> todoService.getById(404L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("404")
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.TODO_NOT_FOUND);
    }

    @Test
    @DisplayName("批量删除：ID 列表为空时抛出 BAD_REQUEST")
    void shouldThrowWhenBatchDeleteIdsEmpty() {
        assertThatThrownBy(() -> todoService.deleteBatch(List.of()))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.BAD_REQUEST);

        then(todoRepository).should(never()).deleteAll(anyCollection());
    }

    @Test
    @DisplayName("批量删除：包含不存在的 ID 时整体失败，不执行删除")
    void shouldThrowWhenBatchDeleteContainsMissingId() {
        TodoItem existing = TodoItem.builder().title("任务").build();
        existing.setId(1L);
        given(todoRepository.findAllById(any())).willReturn(List.of(existing));

        assertThatThrownBy(() -> todoService.deleteBatch(List.of(1L, 2L)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("2")
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.TODO_NOT_FOUND);

        then(todoRepository).should(never()).deleteAll(anyCollection());
    }

    @Test
    @DisplayName("批量删除：全部有效时返回删除条数")
    void shouldDeleteBatchWhenAllIdsValid() {
        TodoItem first = TodoItem.builder().title("任务一").build();
        first.setId(1L);
        TodoItem second = TodoItem.builder().title("任务二").build();
        second.setId(2L);
        given(todoRepository.findAllById(any())).willReturn(List.of(first, second));

        int deleted = todoService.deleteBatch(List.of(1L, 2L, 2L));

        assertThat(deleted).isEqualTo(2);
        then(todoRepository).should().deleteAll(anyCollection());
    }

    @Test
    @DisplayName("统计：完成率保留两位小数，缺失状态补 0")
    void shouldComputeCompletionRate() {
        given(todoRepository.count()).willReturn(3L);
        given(todoRepository.countGroupByStatus()).willReturn(List.of());
        given(todoRepository.countGroupByPriority()).willReturn(List.of());
        given(todoRepository.countByDueDateBeforeAndStatusIn(any(), anyCollection())).willReturn(1L);
        given(todoRepository.countByDueDateAndStatusIn(any(), anyCollection())).willReturn(0L);
        given(todoRepository.countByAssigneeIsNull()).willReturn(2L);
        given(todoRepository.findRecentWithAssignee(any())).willReturn(List.of());

        TodoStatisticsResponse statistics = todoService.statistics();

        // 没有 DONE 数据时完成率应为 0，且所有枚举值都要出现
        assertThat(statistics.total()).isEqualTo(3L);
        assertThat(statistics.completionRate()).isEqualTo(0.0D);
        assertThat(statistics.overdueCount()).isEqualTo(1L);
        assertThat(statistics.unassignedCount()).isEqualTo(2L);
        assertThat(statistics.statusCounts())
                .containsKeys(TodoStatus.values())
                .containsEntry(TodoStatus.DONE, 0L);
        assertThat(statistics.priorityCounts()).containsKeys(TodoPriority.values());
        assertThat(statistics.recentTodos()).isEmpty();
    }

    @Test
    @DisplayName("统计：总数为 0 时完成率不抛除零异常")
    void shouldReturnZeroRateWhenNoData() {
        given(todoRepository.count()).willReturn(0L);
        given(todoRepository.countGroupByStatus()).willReturn(List.of());
        given(todoRepository.countGroupByPriority()).willReturn(List.of());
        given(todoRepository.countByDueDateBeforeAndStatusIn(any(), anyCollection())).willReturn(0L);
        given(todoRepository.countByDueDateAndStatusIn(any(), anyCollection())).willReturn(0L);
        given(todoRepository.countByAssigneeIsNull()).willReturn(0L);
        given(todoRepository.findRecentWithAssignee(any())).willReturn(List.of());

        assertThat(todoService.statistics().completionRate()).isZero();
    }

    /**
     * 构造一个测试用负责人。
     */
    private User buildUser() {
        User user = User.builder()
                .username("tester")
                .nickname("测试员")
                .email("tester@taskhub.local")
                .role(UserRole.MEMBER)
                .status(UserStatus.ACTIVE)
                .build();
        user.setId(1L);
        return user;
    }
}
