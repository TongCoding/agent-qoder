package com.example.taskhub.repository.projection;

import com.example.taskhub.enums.UserStatus;

/**
 * 按账号状态分组的计数投影。
 *
 * <p>对应 JPQL：{@code select u.status as status, count(u) as total from User u group by u.status}</p>
 *
 * @author TaskHub
 */
public interface UserStatusCount {

    /** 分组的状态值 */
    UserStatus getStatus();

    /** 该状态下的用户数量 */
    Long getTotal();
}
