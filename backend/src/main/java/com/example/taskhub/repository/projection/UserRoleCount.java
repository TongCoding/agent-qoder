package com.example.taskhub.repository.projection;

import com.example.taskhub.enums.UserRole;

/**
 * 按角色分组的计数投影。
 *
 * @author TaskHub
 */
public interface UserRoleCount {

    /** 分组的角色值 */
    UserRole getRole();

    /** 该角色下的用户数量 */
    Long getTotal();
}
