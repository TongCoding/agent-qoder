package com.example.taskhub.controller;

import com.example.taskhub.common.ApiResponse;
import com.example.taskhub.common.PageResult;
import com.example.taskhub.dto.user.UserCreateRequest;
import com.example.taskhub.dto.user.UserOptionResponse;
import com.example.taskhub.dto.user.UserQueryRequest;
import com.example.taskhub.dto.user.UserResponse;
import com.example.taskhub.dto.user.UserStatisticsResponse;
import com.example.taskhub.dto.user.UserStatusUpdateRequest;
import com.example.taskhub.dto.user.UserUpdateRequest;
import com.example.taskhub.service.UserService;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户管理 REST 接口。
 *
 * <p>基础路径：{@code /api/v1/users}</p>
 *
 * <p>本示例聚焦于分层结构与规范实践，未接入 Spring Security，因此接口无鉴权。
 * 真实项目中应在网关或过滤器链中补充认证与授权逻辑。</p>
 *
 * @author TaskHub
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
@Tag(name = "02. 用户管理", description = "用户的增删改查、状态变更与统计")
public class UserController {

    private final UserService userService;

    /**
     * 分页查询用户列表。
     */
    @GetMapping
    @Operation(summary = "分页查询用户列表",
            description = "支持关键字（用户名/昵称/邮箱）、角色、状态、部门组合筛选；"
                    + "响应中的 todoCount 由服务端批量聚合得出，不会触发 N+1 查询")
    public ApiResponse<PageResult<UserResponse>> page(@Valid @ParameterObject UserQueryRequest query) {
        log.debug("分页查询用户 | {}", query);
        return ApiResponse.success(userService.page(query));
    }

    /**
     * 查询用户详情。
     */
    @GetMapping("/{id}")
    @Operation(summary = "查询用户详情")
    public ApiResponse<UserResponse> getById(
            @Parameter(description = "用户 ID", example = "1", required = true) @PathVariable Long id) {
        return ApiResponse.success(userService.getById(id));
    }

    /**
     * 查询用户下拉选项（仅启用状态用户）。
     */
    @GetMapping("/options")
    @Operation(summary = "查询用户下拉选项",
            description = "返回状态为 ACTIVE 的用户精简信息，用于待办的负责人选择器")
    public ApiResponse<List<UserOptionResponse>> listOptions() {
        return ApiResponse.success(userService.listOptions());
    }

    /**
     * 查询用户统计数据（仪表盘）。
     */
    @GetMapping("/statistics")
    @Operation(summary = "查询用户统计", description = "返回用户总数、各状态与各角色的数量分布")
    public ApiResponse<UserStatisticsResponse> statistics() {
        return ApiResponse.success(userService.statistics());
    }

    /**
     * 创建用户。
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "创建用户", description = "用户名与邮箱需全局唯一，重复时返回 code = 40901 / 40902")
    public ApiResponse<UserResponse> create(@Valid @RequestBody UserCreateRequest request) {
        return ApiResponse.success("创建成功", userService.create(request));
    }

    /**
     * 全量更新用户。
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新用户", description = "全量更新语义；用户名创建后不可修改，故不在请求体中")
    public ApiResponse<UserResponse> update(
            @Parameter(description = "用户 ID", example = "1", required = true) @PathVariable Long id,
            @Valid @RequestBody UserUpdateRequest request) {
        return ApiResponse.success("更新成功", userService.update(id, request));
    }

    /**
     * 变更用户账号状态。
     */
    @PatchMapping("/{id}/status")
    @Operation(summary = "变更用户状态", description = "PATCH 语义，仅修改 status 字段（ACTIVE / DISABLED / LOCKED）")
    public ApiResponse<UserResponse> changeStatus(
            @Parameter(description = "用户 ID", example = "1", required = true) @PathVariable Long id,
            @Valid @RequestBody UserStatusUpdateRequest request) {
        return ApiResponse.success("状态已更新", userService.changeStatus(id, request));
    }

    /**
     * 删除用户。
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除用户",
            description = "若该用户名下仍存在待办事项，则返回 code = 40903，需先转移或删除关联待办")
    public ApiResponse<Void> delete(
            @Parameter(description = "用户 ID", example = "1", required = true) @PathVariable Long id) {
        userService.delete(id);
        return ApiResponse.success("删除成功", null);
    }
}
