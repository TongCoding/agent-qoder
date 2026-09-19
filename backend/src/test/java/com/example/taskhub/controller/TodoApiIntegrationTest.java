package com.example.taskhub.controller;

import com.example.taskhub.dto.todo.TodoCreateRequest;
import com.example.taskhub.dto.todo.TodoStatusUpdateRequest;
import com.example.taskhub.dto.todo.TodoUpdateRequest;
import com.example.taskhub.entity.TodoItem;
import com.example.taskhub.entity.User;
import com.example.taskhub.enums.TodoPriority;
import com.example.taskhub.enums.TodoStatus;
import com.example.taskhub.enums.UserRole;
import com.example.taskhub.enums.UserStatus;
import com.example.taskhub.repository.TodoRepository;
import com.example.taskhub.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 待办事项接口集成测试。
 *
 * <p>使用 {@code @SpringBootTest} 启动完整上下文 + MockMvc 发起真实 HTTP 请求，
 * 覆盖「Controller -> Service -> Repository -> H2」的完整链路，同时验证统一响应体、
 * 参数校验与全局异常处理是否按预期工作。</p>
 *
 * <p>类级别的 {@code @Transactional} 让每个用例执行后自动回滚，保证用例之间数据隔离。</p>
 *
 * @author TaskHub
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class TodoApiIntegrationTest {

    private static final String BASE_URL = "/api/v1/todos";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TodoRepository todoRepository;

    @Autowired
    private UserRepository userRepository;

    /** 测试用负责人 */
    private User assignee;

    /**
     * 准备 3 条覆盖不同状态、优先级与逾期情况的待办数据。
     */
    @BeforeEach
    void setUp() {
        // 先删子表再删主表，避免外键约束报错
        todoRepository.deleteAll();
        userRepository.deleteAll();

        assignee = userRepository.save(User.builder()
                .username("tester")
                .nickname("测试员")
                .email("tester@taskhub.local")
                .department("质量部")
                .role(UserRole.MEMBER)
                .status(UserStatus.ACTIVE)
                .build());

        todoRepository.save(TodoItem.builder()
                .title("编写集成测试")
                .description("覆盖增删改查与异常分支")
                .status(TodoStatus.IN_PROGRESS)
                .priority(TodoPriority.HIGH)
                .dueDate(LocalDate.now().plusDays(2))
                .assignee(assignee)
                .build());

        todoRepository.save(TodoItem.builder()
                .title("已逾期的任务")
                .status(TodoStatus.PENDING)
                .priority(TodoPriority.URGENT)
                .dueDate(LocalDate.now().minusDays(1))
                .assignee(assignee)
                .build());

        todoRepository.save(TodoItem.builder()
                .title("已完成的任务")
                .status(TodoStatus.DONE)
                .priority(TodoPriority.LOW)
                .dueDate(LocalDate.now().minusDays(3))
                .completedAt(LocalDate.now().minusDays(3).atTime(18, 0))
                .build());
    }

    // ==================================================================
    // 查询类接口
    // ==================================================================

    @Test
    @DisplayName("分页查询：返回统一响应体与分页元信息")
    void shouldReturnPagedTodos() throws Exception {
        mockMvc.perform(get(BASE_URL).param("page", "1").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("操作成功"))
                .andExpect(jsonPath("$.timestamp").value(notNullValue()))
                .andExpect(jsonPath("$.data.content", hasSize(2)))
                .andExpect(jsonPath("$.data.totalElements").value(3))
                .andExpect(jsonPath("$.data.totalPages").value(2))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.size").value(2))
                .andExpect(jsonPath("$.data.first").value(true))
                .andExpect(jsonPath("$.data.last").value(false));
    }

    @Test
    @DisplayName("分页查询：页码越界返回空列表而非报错")
    void shouldReturnEmptyPageWhenPageOutOfRange() throws Exception {
        mockMvc.perform(get(BASE_URL).param("page", "99").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.content", hasSize(0)))
                .andExpect(jsonPath("$.data.totalElements").value(3))
                .andExpect(jsonPath("$.data.empty").value(true));
    }

    @Test
    @DisplayName("条件筛选：按状态过滤")
    void shouldFilterByStatus() throws Exception {
        mockMvc.perform(get(BASE_URL).param("status", "DONE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].title").value("已完成的任务"))
                .andExpect(jsonPath("$.data.content[0].completedAt").value(notNullValue()));
    }

    @Test
    @DisplayName("条件筛选：关键字模糊匹配标题")
    void shouldFilterByKeyword() throws Exception {
        mockMvc.perform(get(BASE_URL).param("keyword", "集成"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].title").value("编写集成测试"));
    }

    @Test
    @DisplayName("条件筛选：只看逾期任务")
    void shouldFilterOverdueOnly() throws Exception {
        mockMvc.perform(get(BASE_URL).param("overdue", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].overdue").value(true));
    }

    @Test
    @DisplayName("条件筛选：按负责人过滤并返回负责人信息")
    void shouldFilterByAssigneeAndReturnAssigneeInfo() throws Exception {
        mockMvc.perform(get(BASE_URL).param("assigneeId", String.valueOf(assignee.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.content[0].assignee.username").value("tester"))
                .andExpect(jsonPath("$.data.content[0].assignee.nickname").value("测试员"));
    }

    @Test
    @DisplayName("条件筛选：只看未分配负责人的任务")
    void shouldFilterUnassigned() throws Exception {
        mockMvc.perform(get(BASE_URL).param("unassigned", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                // 负责人为空时，因 default-property-inclusion=non_null，该字段不会出现在响应中
                .andExpect(jsonPath("$.data.content[0].assignee").doesNotExist());
    }

    @Test
    @DisplayName("排序：按截止日期升序")
    void shouldSortByDueDateAsc() throws Exception {
        mockMvc.perform(get(BASE_URL)
                        .param("sortBy", "dueDate")
                        .param("sortDir", "asc")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].title").value("已完成的任务"));
    }

    @Test
    @DisplayName("排序：非白名单字段自动回退为默认排序，不抛异常")
    void shouldFallbackWhenSortFieldNotInWhitelist() throws Exception {
        mockMvc.perform(get(BASE_URL).param("sortBy", "assignee.password").param("sortDir", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.totalElements").value(3));
    }

    @Test
    @DisplayName("详情：返回完整字段")
    void shouldReturnTodoDetail() throws Exception {
        Long id = todoRepository.findAll().get(0).getId();

        mockMvc.perform(get(BASE_URL + "/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(id))
                .andExpect(jsonPath("$.data.title").value(notNullValue()))
                .andExpect(jsonPath("$.data.status").value(notNullValue()))
                .andExpect(jsonPath("$.data.priority").value(notNullValue()))
                .andExpect(jsonPath("$.data.createdAt").value(notNullValue()));
    }

    @Test
    @DisplayName("统计：返回各维度聚合指标")
    void shouldReturnStatistics() throws Exception {
        mockMvc.perform(get(BASE_URL + "/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(3))
                .andExpect(jsonPath("$.data.statusCounts.DONE").value(1))
                .andExpect(jsonPath("$.data.statusCounts.IN_PROGRESS").value(1))
                .andExpect(jsonPath("$.data.statusCounts.PENDING").value(1))
                .andExpect(jsonPath("$.data.priorityCounts.URGENT").value(1))
                .andExpect(jsonPath("$.data.overdueCount").value(1))
                .andExpect(jsonPath("$.data.unassignedCount").value(1))
                .andExpect(jsonPath("$.data.completionRate").value(33.33))
                .andExpect(jsonPath("$.data.recentTodos", hasSize(3)));
    }

    // ==================================================================
    // 写入类接口
    // ==================================================================

    @Test
    @DisplayName("创建：成功返回 201 与落库数据，缺省状态回退为默认值")
    void shouldCreateTodo() throws Exception {
        TodoCreateRequest request = new TodoCreateRequest(
                "新增的待办", "描述内容", null, null, LocalDate.now().plusDays(7), assignee.getId());

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("创建成功"))
                .andExpect(jsonPath("$.data.id").value(notNullValue()))
                .andExpect(jsonPath("$.data.title").value("新增的待办"))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.priority").value("MEDIUM"))
                .andExpect(jsonPath("$.data.overdue").value(false))
                .andExpect(jsonPath("$.data.assignee.id").value(assignee.getId()));
    }

    @Test
    @DisplayName("创建：标题为空白时返回 400 与字段级错误明细")
    void shouldReturn400WhenTitleBlank() throws Exception {
        TodoCreateRequest request = new TodoCreateRequest("   ", null, null, null, null, null);

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40001))
                .andExpect(jsonPath("$.message").value(containsString("标题不能为空")))
                .andExpect(jsonPath("$.data[0].field").value("title"))
                .andExpect(jsonPath("$.data[0].message").value("标题不能为空"));
    }

    @Test
    @DisplayName("创建：标题超长时返回 400")
    void shouldReturn400WhenTitleTooLong() throws Exception {
        TodoCreateRequest request = new TodoCreateRequest("a".repeat(121), null, null, null, null, null);

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40001))
                .andExpect(jsonPath("$.data[0].field").value("title"));
    }

    @Test
    @DisplayName("创建：负责人不存在时返回 404")
    void shouldReturn404WhenAssigneeNotExists() throws Exception {
        TodoCreateRequest request = new TodoCreateRequest("待办", null, null, null, null, 999_999L);

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40402))
                .andExpect(jsonPath("$.message").value(containsString("负责人不存在")));
    }

    @Test
    @DisplayName("创建：JSON 格式非法时返回 400")
    void shouldReturn400WhenBodyMalformed() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"缺少右括号\""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40002));
    }

    @Test
    @DisplayName("更新：全量更新字段生效，流转 DONE 时自动写入完成时间")
    void shouldUpdateTodo() throws Exception {
        Long id = todoRepository.findAll().get(0).getId();

        TodoUpdateRequest request = new TodoUpdateRequest(
                "修改后的标题", "修改后的描述", TodoStatus.DONE, TodoPriority.LOW,
                LocalDate.now().plusDays(1), null);

        mockMvc.perform(put(BASE_URL + "/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("更新成功"))
                .andExpect(jsonPath("$.data.title").value("修改后的标题"))
                .andExpect(jsonPath("$.data.description").value("修改后的描述"))
                .andExpect(jsonPath("$.data.status").value("DONE"))
                .andExpect(jsonPath("$.data.priority").value("LOW"))
                .andExpect(jsonPath("$.data.completedAt").value(notNullValue()))
                // 负责人被显式置空
                .andExpect(jsonPath("$.data.assignee").doesNotExist());
    }

    @Test
    @DisplayName("更新：目标不存在时返回 404")
    void shouldReturn404WhenUpdateMissingTodo() throws Exception {
        TodoUpdateRequest request = new TodoUpdateRequest(
                "标题", null, TodoStatus.PENDING, TodoPriority.MEDIUM, null, null);

        mockMvc.perform(put(BASE_URL + "/{id}", 999_999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40401));
    }

    @Test
    @DisplayName("状态流转：PATCH 只修改状态并同步完成时间与逾期标记")
    void shouldChangeStatus() throws Exception {
        TodoItem target = todoRepository.findAll().stream()
                .filter(item -> item.getStatus() == TodoStatus.PENDING)
                .findFirst()
                .orElseThrow();

        mockMvc.perform(patch(BASE_URL + "/{id}/status", target.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TodoStatusUpdateRequest(TodoStatus.DONE))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("状态已更新"))
                .andExpect(jsonPath("$.data.status").value("DONE"))
                .andExpect(jsonPath("$.data.completedAt").value(notNullValue()))
                .andExpect(jsonPath("$.data.overdue").value(false));
    }

    @Test
    @DisplayName("删除：成功后再次查询返回 404")
    void shouldDeleteTodo() throws Exception {
        Long id = todoRepository.findAll().get(0).getId();

        mockMvc.perform(delete(BASE_URL + "/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("删除成功"));

        mockMvc.perform(get(BASE_URL + "/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40401));
    }

    @Test
    @DisplayName("批量删除：ID 全部有效时返回删除条数")
    void shouldDeleteBatch() throws Exception {
        String ids = todoRepository.findAll().stream()
                .map(item -> String.valueOf(item.getId()))
                .collect(Collectors.joining(","));

        mockMvc.perform(delete(BASE_URL).param("ids", ids))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").value(3));

        mockMvc.perform(get(BASE_URL))
                .andExpect(jsonPath("$.data.totalElements").value(0));
    }

    @Test
    @DisplayName("批量删除：包含无效 ID 时整体失败并提示缺失项，数据不被删除")
    void shouldFailBatchDeleteWhenSomeIdMissing() throws Exception {
        Long validId = todoRepository.findAll().get(0).getId();

        mockMvc.perform(delete(BASE_URL).param("ids", validId + ",999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40401))
                .andExpect(jsonPath("$.message").value(containsString("999999")));

        // 事务性保证：失败时不应删除任何数据
        mockMvc.perform(get(BASE_URL))
                .andExpect(jsonPath("$.data.totalElements").value(3));
    }

    // ==================================================================
    // 协议层异常
    // ==================================================================

    @Test
    @DisplayName("异常：路径参数类型不匹配返回 400 / 40003")
    void shouldReturn400WhenIdNotNumeric() throws Exception {
        mockMvc.perform(get(BASE_URL + "/{id}", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40003))
                .andExpect(jsonPath("$.message").value(containsString("id")));
    }

    @Test
    @DisplayName("异常：枚举取值非法返回 400")
    void shouldReturn400WhenEnumValueInvalid() throws Exception {
        // 查询对象绑定失败会转成字段级校验错误，因此这里只断言 HTTP 状态与错误码大类
        mockMvc.perform(get(BASE_URL).param("status", "NOT_A_STATUS"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40001));
    }

    @Test
    @DisplayName("异常：分页 size 超出上限返回 400 / 40001")
    void shouldReturn400WhenPageSizeExceedsLimit() throws Exception {
        mockMvc.perform(get(BASE_URL).param("size", "1000"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40001))
                .andExpect(jsonPath("$.data[0].field").value("size"));
    }

    @Test
    @DisplayName("异常：请求方法不支持返回 405")
    void shouldReturn405WhenMethodNotSupported() throws Exception {
        mockMvc.perform(post(BASE_URL + "/statistics"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value(40500));
    }

    @Test
    @DisplayName("异常：未知接口返回 404 且响应体仍为统一结构")
    void shouldReturn404WithUnifiedBodyForUnknownPath() throws Exception {
        mockMvc.perform(get("/api/v1/not-exists"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40400))
                .andExpect(jsonPath("$.message").value(containsString("不存在")));
    }
}
