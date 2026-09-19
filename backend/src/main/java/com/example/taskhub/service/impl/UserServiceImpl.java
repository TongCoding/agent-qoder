package com.example.taskhub.service.impl;

import com.example.taskhub.common.ErrorCode;
import com.example.taskhub.common.PageResult;
import com.example.taskhub.dto.user.UserCreateRequest;
import com.example.taskhub.dto.user.UserOptionResponse;
import com.example.taskhub.dto.user.UserQueryRequest;
import com.example.taskhub.dto.user.UserResponse;
import com.example.taskhub.dto.user.UserStatisticsResponse;
import com.example.taskhub.dto.user.UserStatusUpdateRequest;
import com.example.taskhub.dto.user.UserUpdateRequest;
import com.example.taskhub.entity.User;
import com.example.taskhub.enums.UserRole;
import com.example.taskhub.enums.UserStatus;
import com.example.taskhub.exception.BusinessException;
import com.example.taskhub.mapper.UserMapper;
import com.example.taskhub.repository.TodoRepository;
import com.example.taskhub.repository.UserRepository;
import com.example.taskhub.repository.projection.AssigneeTodoCount;
import com.example.taskhub.repository.specification.UserSpecifications;
import com.example.taskhub.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 用户管理服务实现。
 *
 * <p>事务边界统一放在 Service 层：写操作使用默认事务，读操作标记 {@code readOnly = true}
 * 以便 Hibernate 跳过脏检查、提升查询性能。</p>
 *
 * @author TaskHub
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final TodoRepository todoRepository;

    @Override
    @Transactional
    public UserResponse create(UserCreateRequest request) {
        String username = request.username().trim();
        String email = request.email().trim();

        // 唯一性校验：提前给出业务错误码，而不是等数据库抛唯一约束异常
        if (userRepository.existsByUsername(username)) {
            throw new BusinessException(ErrorCode.USERNAME_DUPLICATED, "用户名已存在：" + username);
        }
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.EMAIL_DUPLICATED, "邮箱已被占用：" + email);
        }

        User saved = userRepository.save(UserMapper.toEntity(request));
        log.info("创建用户成功 | id={} | username={}", saved.getId(), saved.getUsername());
        return UserMapper.toResponse(saved, 0L);
    }

    @Override
    @Transactional
    public UserResponse update(Long id, UserUpdateRequest request) {
        User user = getExistingUser(id);

        String email = request.email().trim();
        if (userRepository.existsByEmailAndIdNot(email, id)) {
            throw new BusinessException(ErrorCode.EMAIL_DUPLICATED, "邮箱已被占用：" + email);
        }

        UserMapper.applyUpdate(user, request);
        // 事务提交时由 JPA 脏检查自动 flush，这里显式 save 以增强可读性
        User saved = userRepository.save(user);
        log.info("更新用户成功 | id={}", saved.getId());
        return UserMapper.toResponse(saved, todoRepository.countByAssigneeId(saved.getId()));
    }

    @Override
    @Transactional
    public UserResponse changeStatus(Long id, UserStatusUpdateRequest request) {
        User user = getExistingUser(id);
        if (user.getStatus() == request.status()) {
            // 状态未变化时直接返回，避免无意义的更新与乐观锁版本递增
            return UserMapper.toResponse(user, todoRepository.countByAssigneeId(id));
        }
        user.setStatus(request.status());
        User saved = userRepository.save(user);
        log.info("变更用户状态 | id={} | status={}", id, saved.getStatus());
        return UserMapper.toResponse(saved, todoRepository.countByAssigneeId(id));
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getById(Long id) {
        User user = getExistingUser(id);
        return UserMapper.toResponse(user, todoRepository.countByAssigneeId(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<UserResponse> page(UserQueryRequest query) {
        UserQueryRequest condition = query == null ? new UserQueryRequest() : query;
        Pageable pageable = condition.toPageable(
                UserQueryRequest.DEFAULT_SORT_FIELD,
                UserQueryRequest.SORTABLE_FIELDS
        );

        Page<User> page = userRepository.findAll(UserSpecifications.filter(condition), pageable);
        if (page.isEmpty()) {
            return PageResult.from(page, List.of());
        }

        // 批量统计名下待办数：一次聚合查询覆盖整页数据，避免逐行 count 造成 N+1
        Map<Long, Long> todoCountMap = loadTodoCounts(page.getContent());

        List<UserResponse> content = page.getContent().stream()
                .map(user -> UserMapper.toResponse(user, todoCountMap.getOrDefault(user.getId(), 0L)))
                .toList();

        return PageResult.from(page, content);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        User user = getExistingUser(id);

        long relatedTodos = todoRepository.countByAssigneeId(id);
        if (relatedTodos > 0) {
            throw new BusinessException(ErrorCode.RESOURCE_IN_USE,
                    String.format("该用户名下仍有 %d 条待办事项，请先转移或删除后再操作", relatedTodos));
        }

        userRepository.delete(user);
        log.info("删除用户成功 | id={} | username={}", id, user.getUsername());
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserOptionResponse> listOptions() {
        return userRepository.findByStatusOrderByUsernameAsc(UserStatus.ACTIVE).stream()
                .map(UserMapper::toOption)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public UserStatisticsResponse statistics() {
        Map<UserStatus, Long> statusCounts = new EnumMap<>(UserStatus.class);
        // 先把所有枚举值补 0，保证前端图表的类目完整
        for (UserStatus status : UserStatus.values()) {
            statusCounts.put(status, 0L);
        }
        userRepository.countGroupByStatus()
                .forEach(item -> statusCounts.put(item.getStatus(), item.getTotal()));

        Map<UserRole, Long> roleCounts = new EnumMap<>(UserRole.class);
        for (UserRole role : UserRole.values()) {
            roleCounts.put(role, 0L);
        }
        userRepository.countGroupByRole()
                .forEach(item -> roleCounts.put(item.getRole(), item.getTotal()));

        return new UserStatisticsResponse(userRepository.count(), statusCounts, roleCounts);
    }

    // ==================================================================
    // 内部工具方法
    // ==================================================================

    /**
     * 按 ID 加载用户，不存在时抛出业务异常。
     */
    private User getExistingUser(Long id) {
        if (id == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "用户 ID 不能为空");
        }
        return userRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.USER_NOT_FOUND, id));
    }

    /**
     * 批量查询一组用户名下的待办数量。
     *
     * @param users 当前页用户列表
     * @return userId -> todoCount 映射，不存在待办的用户不会出现在结果中
     */
    private Map<Long, Long> loadTodoCounts(List<User> users) {
        List<Long> userIds = users.stream().map(User::getId).toList();
        return todoRepository.countGroupByAssignee(userIds).stream()
                .collect(Collectors.toMap(
                        AssigneeTodoCount::getAssigneeId,
                        AssigneeTodoCount::getTotal,
                        // 理论上分组结果不会出现重复 key，此处仅作防御
                        (existing, replacement) -> existing,
                        HashMap::new
                ));
    }
}
