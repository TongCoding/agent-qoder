package com.example.taskhub.mapper;

import com.example.taskhub.dto.user.UserCreateRequest;
import com.example.taskhub.dto.user.UserOptionResponse;
import com.example.taskhub.dto.user.UserResponse;
import com.example.taskhub.dto.user.UserUpdateRequest;
import com.example.taskhub.entity.User;

/**
 * 用户实体与 DTO 之间的手动转换器。
 *
 * <p>为什么不直接返回实体？</p>
 * <ul>
 *   <li>隔离持久化模型与接口契约，实体加字段不会意外泄漏到 API；</li>
 *   <li>避免 Jackson 序列化懒加载代理时触发 {@code LazyInitializationException}；</li>
 *   <li>便于在响应中追加派生字段（如 {@code todoCount}）。</li>
 * </ul>
 *
 * <p>项目规模变大后可替换为 MapStruct，在编译期生成等价代码。</p>
 *
 * @author TaskHub
 */
public final class UserMapper {

    private UserMapper() {
        // 工具类，禁止实例化
    }

    /**
     * 创建请求 -> 实体。
     *
     * <p>{@code id}、{@code createdAt} 等字段交由 JPA 与审计机制填充。</p>
     */
    public static User toEntity(UserCreateRequest request) {
        return User.builder()
                .username(request.username().trim())
                .nickname(request.nickname())
                .email(request.email().trim())
                .phone(emptyToNull(request.phone()))
                .department(emptyToNull(request.department()))
                .role(request.role())
                .status(request.status())
                .remark(emptyToNull(request.remark()))
                .build();
    }

    /**
     * 将更新请求的字段写回已存在的实体（原地修改，保证 JPA 脏检查生效）。
     *
     * @param target  数据库中已加载的实体
     * @param request 更新请求
     */
    public static void applyUpdate(User target, UserUpdateRequest request) {
        target.setNickname(request.nickname());
        target.setEmail(request.email().trim());
        target.setPhone(emptyToNull(request.phone()));
        target.setDepartment(emptyToNull(request.department()));
        target.setRole(request.role());
        target.setStatus(request.status());
        target.setRemark(emptyToNull(request.remark()));
    }

    /**
     * 实体 -> 响应 DTO。
     *
     * @param user      用户实体
     * @param todoCount 名下待办数量
     */
    public static UserResponse toResponse(User user, long todoCount) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getEmail(),
                user.getPhone(),
                user.getDepartment(),
                user.getRole(),
                user.getStatus(),
                user.getRemark(),
                todoCount,
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    /**
     * 实体 -> 下拉选项 DTO。
     */
    public static UserOptionResponse toOption(User user) {
        return new UserOptionResponse(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getDepartment()
        );
    }

    /**
     * 将空字符串或纯空白统一转换为 {@code null}，避免数据库中存入无意义的空串。
     */
    private static String emptyToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}
