package com.example.taskhub.controller;

import com.example.taskhub.dto.user.UserCreateRequest;
import com.example.taskhub.dto.user.UserStatusUpdateRequest;
import com.example.taskhub.dto.user.UserUpdateRequest;
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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
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
 * 用户管理接口集成测试。
 *
 * <p>除常规 CRUD 外，重点覆盖唯一性约束、关联数据保护与字段级校验错误响应。</p>
 *
 * @author TaskHub
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UserApiIntegrationTest {

    private static final String BASE_URL = "/api/v1/users";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TodoRepository todoRepository;

    /** 名下挂有待办的用户，用于验证删除保护 */
    private User busyUser;

    @BeforeEach
    void setUp() {
        todoRepository.deleteAll();
        userRepository.deleteAll();

        busyUser = userRepository.save(User.builder()
                .username("busyuser")
                .nickname("忙碌的张三")
                .email("busy@taskhub.local")
                .department("研发部")
                .role(UserRole.MANAGER)
                .status(UserStatus.ACTIVE)
                .build());

        userRepository.save(User.builder()
                .username("idleuser")
                .nickname("空闲的李四")
                .email("idle@taskhub.local")
                .department("设计部")
                .role(UserRole.MEMBER)
                .status(UserStatus.DISABLED)
                .build());

        todoRepository.save(TodoItem.builder()
                .title("张三名下的待办")
                .status(TodoStatus.IN_PROGRESS)
                .priority(TodoPriority.HIGH)
                .dueDate(LocalDate.now().plusDays(1))
                .assignee(busyUser)
                .build());
    }

    // ==================================================================
    // 查询
    // ==================================================================

    @Test
    @DisplayName("分页查询：todoCount 由服务端批量聚合填充")
    void shouldReturnPagedUsersWithTodoCount() throws Exception {
        // 显式指定按用户名升序，避免依赖创建时间的先后（同一事务内时间戳可能相同）
        mockMvc.perform(get(BASE_URL)
                        .param("page", "1")
                        .param("size", "10")
                        .param("sortBy", "username")
                        .param("sortDir", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.content", hasSize(2)))
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.content[0].username").value("busyuser"))
                .andExpect(jsonPath("$.data.content[0].todoCount").value(1))
                .andExpect(jsonPath("$.data.content[1].username").value("idleuser"))
                .andExpect(jsonPath("$.data.content[1].todoCount").value(0));
    }

    @Test
    @DisplayName("条件筛选：按角色过滤")
    void shouldFilterByRole() throws Exception {
        mockMvc.perform(get(BASE_URL).param("role", "MANAGER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].username").value("busyuser"));
    }

    @Test
    @DisplayName("条件筛选：关键字模糊匹配昵称")
    void shouldFilterByKeyword() throws Exception {
        mockMvc.perform(get(BASE_URL).param("keyword", "李四"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].username").value("idleuser"));
    }

    @Test
    @DisplayName("条件筛选：按部门过滤")
    void shouldFilterByDepartment() throws Exception {
        mockMvc.perform(get(BASE_URL).param("department", "研发部"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].department").value("研发部"));
    }

    @Test
    @DisplayName("详情：返回用户完整信息与待办数")
    void shouldReturnUserDetail() throws Exception {
        mockMvc.perform(get(BASE_URL + "/{id}", busyUser.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(busyUser.getId()))
                .andExpect(jsonPath("$.data.username").value("busyuser"))
                .andExpect(jsonPath("$.data.email").value("busy@taskhub.local"))
                .andExpect(jsonPath("$.data.todoCount").value(1))
                .andExpect(jsonPath("$.data.createdAt").value(notNullValue()));
    }

    @Test
    @DisplayName("下拉选项：只返回启用状态的用户")
    void shouldReturnOnlyActiveUsersAsOptions() throws Exception {
        mockMvc.perform(get(BASE_URL + "/options"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].username").value("busyuser"))
                .andExpect(jsonPath("$.data[0].nickname").value("忙碌的张三"));
    }

    @Test
    @DisplayName("统计：返回状态与角色分布")
    void shouldReturnStatistics() throws Exception {
        mockMvc.perform(get(BASE_URL + "/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.statusCounts.ACTIVE").value(1))
                .andExpect(jsonPath("$.data.statusCounts.DISABLED").value(1))
                // 未被使用的枚举值也应补 0，保证前端图表类目完整
                .andExpect(jsonPath("$.data.statusCounts.LOCKED").value(0))
                .andExpect(jsonPath("$.data.roleCounts.MANAGER").value(1))
                .andExpect(jsonPath("$.data.roleCounts.MEMBER").value(1))
                .andExpect(jsonPath("$.data.roleCounts.ADMIN").value(0));
    }

    // ==================================================================
    // 写入
    // ==================================================================

    @Test
    @DisplayName("创建：成功返回 201")
    void shouldCreateUser() throws Exception {
        UserCreateRequest request = new UserCreateRequest(
                "newuser", "新用户", "new@taskhub.local", "13900139000",
                "测试部", UserRole.MEMBER, UserStatus.ACTIVE, "由集成测试创建");

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("创建成功"))
                .andExpect(jsonPath("$.data.id").value(notNullValue()))
                .andExpect(jsonPath("$.data.username").value("newuser"))
                .andExpect(jsonPath("$.data.todoCount").value(0));
    }

    @Test
    @DisplayName("创建：用户名重复返回 409 / 40901")
    void shouldReturn409WhenUsernameDuplicated() throws Exception {
        UserCreateRequest request = new UserCreateRequest(
                "busyuser", "重复用户", "other@taskhub.local", null,
                null, UserRole.MEMBER, UserStatus.ACTIVE, null);

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(40901))
                .andExpect(jsonPath("$.message").value(containsString("busyuser")));
    }

    @Test
    @DisplayName("创建：邮箱重复返回 409 / 40902")
    void shouldReturn409WhenEmailDuplicated() throws Exception {
        UserCreateRequest request = new UserCreateRequest(
                "anothername", "另一个用户", "busy@taskhub.local", null,
                null, UserRole.MEMBER, UserStatus.ACTIVE, null);

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(40902));
    }

    @Test
    @DisplayName("创建：多个字段格式非法时一次性返回全部字段错误")
    void shouldReturn400WithMultipleFieldErrors() throws Exception {
        UserCreateRequest request = new UserCreateRequest(
                "ab", "昵称", "not-an-email", "12345",
                null, UserRole.MEMBER, UserStatus.ACTIVE, null);

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40001))
                // username 长度不足、email 格式错误、phone 格式错误，至少 3 条
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(3))))
                .andExpect(jsonPath("$.message").value(containsString("用户名")))
                .andExpect(jsonPath("$.message").value(containsString("邮箱")));
    }

    @Test
    @DisplayName("更新：字段生效且 todoCount 正确")
    void shouldUpdateUser() throws Exception {
        UserUpdateRequest request = new UserUpdateRequest(
                "张三（已改名）", "busy@taskhub.local", "13700137000",
                "架构组", UserRole.ADMIN, UserStatus.ACTIVE, "晋升为管理员");

        mockMvc.perform(put(BASE_URL + "/{id}", busyUser.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("更新成功"))
                .andExpect(jsonPath("$.data.nickname").value("张三（已改名）"))
                .andExpect(jsonPath("$.data.department").value("架构组"))
                .andExpect(jsonPath("$.data.role").value("ADMIN"))
                .andExpect(jsonPath("$.data.todoCount").value(1));
    }

    @Test
    @DisplayName("更新：邮箱被他人占用返回 409")
    void shouldReturn409WhenUpdateEmailOwnedByOthers() throws Exception {
        UserUpdateRequest request = new UserUpdateRequest(
                "张三", "idle@taskhub.local", null,
                null, UserRole.MANAGER, UserStatus.ACTIVE, null);

        mockMvc.perform(put(BASE_URL + "/{id}", busyUser.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(40902));
    }

    @Test
    @DisplayName("更新：用户不存在返回 404 / 40402")
    void shouldReturn404WhenUpdateMissingUser() throws Exception {
        UserUpdateRequest request = new UserUpdateRequest(
                "昵称", "nobody@taskhub.local", null,
                null, UserRole.MEMBER, UserStatus.ACTIVE, null);

        mockMvc.perform(put(BASE_URL + "/{id}", 999_999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40402));
    }

    @Test
    @DisplayName("状态变更：PATCH 只更新 status")
    void shouldChangeStatus() throws Exception {
        mockMvc.perform(patch(BASE_URL + "/{id}/status", busyUser.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UserStatusUpdateRequest(UserStatus.LOCKED))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("状态已更新"))
                .andExpect(jsonPath("$.data.status").value("LOCKED"))
                // 其他字段保持不变
                .andExpect(jsonPath("$.data.username").value("busyuser"))
                .andExpect(jsonPath("$.data.role").value("MANAGER"));
    }

    @Test
    @DisplayName("删除保护：名下有待办时返回 409 / 40903")
    void shouldReturn409WhenDeleteUserWithTodos() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/{id}", busyUser.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(40903))
                .andExpect(jsonPath("$.message").value(containsString("1 条待办")));

        // 用户仍然存在
        mockMvc.perform(get(BASE_URL + "/{id}", busyUser.getId()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("删除：无关联待办时可以成功删除")
    void shouldDeleteUserWithoutTodos() throws Exception {
        Long idleUserId = userRepository.findByUsername("idleuser").orElseThrow().getId();

        mockMvc.perform(delete(BASE_URL + "/{id}", idleUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("删除成功"));

        mockMvc.perform(get(BASE_URL + "/{id}", idleUserId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40402));
    }

    @Test
    @DisplayName("异常：路径参数非数字返回 400 / 40003")
    void shouldReturn400WhenIdNotNumeric() throws Exception {
        mockMvc.perform(get(BASE_URL + "/{id}", "not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40003));
    }
}
