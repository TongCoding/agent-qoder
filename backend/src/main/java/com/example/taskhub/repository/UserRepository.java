package com.example.taskhub.repository;

import com.example.taskhub.entity.User;
import com.example.taskhub.enums.UserRole;
import com.example.taskhub.enums.UserStatus;
import com.example.taskhub.repository.projection.UserRoleCount;
import com.example.taskhub.repository.projection.UserStatusCount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 用户数据访问接口。
 *
 * <p>继承 {@link JpaSpecificationExecutor} 以支持动态条件查询（关键字 / 角色 / 状态组合过滤）。</p>
 *
 * @author TaskHub
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    /**
     * 按用户名精确查询。
     */
    Optional<User> findByUsername(String username);

    /**
     * 用户名是否已存在（用于新增时的唯一性校验）。
     */
    boolean existsByUsername(String username);

    /**
     * 邮箱是否已存在（用于新增时的唯一性校验）。
     */
    boolean existsByEmail(String email);

    /**
     * 邮箱是否被<b>其他</b>用户占用（用于更新时的唯一性校验，需排除自身）。
     *
     * @param email 待校验邮箱
     * @param id    当前用户 ID
     */
    boolean existsByEmailAndIdNot(String email, Long id);

    /**
     * 查询指定状态的启用用户，按用户名升序，用于下拉选择。
     */
    List<User> findByStatusOrderByUsernameAsc(UserStatus status);

    /**
     * 统计指定状态的用户数量。
     */
    long countByStatus(UserStatus status);

    /**
     * 统计指定角色的用户数量。
     */
    long countByRole(UserRole role);

    /**
     * 按状态分组统计用户数。
     */
    @Query("select u.status as status, count(u) as total from User u group by u.status")
    List<UserStatusCount> countGroupByStatus();

    /**
     * 按角色分组统计用户数。
     */
    @Query("select u.role as role, count(u) as total from User u group by u.role")
    List<UserRoleCount> countGroupByRole();
}
