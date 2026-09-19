package com.example.taskhub.entity;

import com.example.taskhub.enums.UserRole;
import com.example.taskhub.enums.UserStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 用户实体。
 *
 * <p>表名使用 {@code sys_user} 而非 {@code user}，因为 {@code USER} 在 H2 / PostgreSQL 等
 * 数据库中是保留关键字，直接使用会导致建表失败。</p>
 *
 * @author TaskHub
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "sys_user",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_username", columnNames = "username"),
                @UniqueConstraint(name = "uk_user_email", columnNames = "email")
        },
        indexes = {
                @Index(name = "idx_user_status", columnList = "status"),
                @Index(name = "idx_user_role", columnList = "role")
        }
)
public class User extends BaseEntity {

    /** 登录用户名，全局唯一 */
    @Column(name = "username", nullable = false, length = 50)
    private String username;

    /** 昵称 / 显示名 */
    @Column(name = "nickname", length = 50)
    private String nickname;

    /** 邮箱，全局唯一 */
    @Column(name = "email", nullable = false, length = 100)
    private String email;

    /** 手机号 */
    @Column(name = "phone", length = 20)
    private String phone;

    /** 所属部门 */
    @Column(name = "department", length = 50)
    private String department;

    /** 角色 */
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    @Builder.Default
    private UserRole role = UserRole.MEMBER;

    /** 账号状态 */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private UserStatus status = UserStatus.ACTIVE;

    /** 备注 */
    @Column(name = "remark", length = 255)
    private String remark;

    /**
     * 乐观锁版本号。
     *
     * <p>并发更新同一用户时，后提交的事务会抛出 {@code OptimisticLockingFailureException}，
     * 避免「后写覆盖先写」的数据丢失问题。</p>
     */
    @Version
    @Column(name = "version")
    private Long version;

    /**
     * 账号是否处于可用状态。
     */
    public boolean isEnabled() {
        return this.status == UserStatus.ACTIVE;
    }
}
