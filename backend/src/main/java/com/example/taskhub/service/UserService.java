package com.example.taskhub.service;

import com.example.taskhub.common.PageResult;
import com.example.taskhub.dto.user.UserCreateRequest;
import com.example.taskhub.dto.user.UserOptionResponse;
import com.example.taskhub.dto.user.UserQueryRequest;
import com.example.taskhub.dto.user.UserResponse;
import com.example.taskhub.dto.user.UserStatisticsResponse;
import com.example.taskhub.dto.user.UserStatusUpdateRequest;
import com.example.taskhub.dto.user.UserUpdateRequest;

import java.util.List;

/**
 * 用户管理服务接口。
 *
 * <p>面向 Controller 层暴露业务能力，屏蔽 JPA 实体与持久化细节。</p>
 *
 * @author TaskHub
 */
public interface UserService {

    /**
     * 创建用户。
     *
     * @param request 创建请求
     * @return 创建后的用户信息（含数据库生成的 ID 与时间戳）
     * @throws com.example.taskhub.exception.BusinessException 用户名或邮箱已存在时抛出
     */
    UserResponse create(UserCreateRequest request);

    /**
     * 全量更新用户信息。
     *
     * @param id      用户 ID
     * @param request 更新请求
     * @return 更新后的用户信息
     * @throws com.example.taskhub.exception.BusinessException 用户不存在或邮箱被他人占用时抛出
     */
    UserResponse update(Long id, UserUpdateRequest request);

    /**
     * 仅更新账号状态（启用 / 停用 / 锁定）。
     *
     * @param id      用户 ID
     * @param request 状态更新请求
     * @return 更新后的用户信息
     */
    UserResponse changeStatus(Long id, UserStatusUpdateRequest request);

    /**
     * 查询用户详情。
     *
     * @param id 用户 ID
     * @return 用户信息
     * @throws com.example.taskhub.exception.BusinessException 用户不存在时抛出
     */
    UserResponse getById(Long id);

    /**
     * 按条件分页查询用户。
     *
     * @param query 查询条件（分页、排序、关键字、角色、状态、部门）
     * @return 分页结果
     */
    PageResult<UserResponse> page(UserQueryRequest query);

    /**
     * 删除用户。
     *
     * <p>若该用户名下仍存在待办事项，则拒绝删除，避免产生孤儿数据。</p>
     *
     * @param id 用户 ID
     * @throws com.example.taskhub.exception.BusinessException 用户不存在或存在关联待办时抛出
     */
    void delete(Long id);

    /**
     * 查询可用于下拉选择的启用用户列表（不分页）。
     *
     * @return 精简用户信息列表
     */
    List<UserOptionResponse> listOptions();

    /**
     * 统计用户分布情况，用于仪表盘。
     *
     * @return 统计结果
     */
    UserStatisticsResponse statistics();
}
