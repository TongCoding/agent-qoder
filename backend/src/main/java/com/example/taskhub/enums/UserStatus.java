package com.example.taskhub.enums;

/**
 * 用户账号状态。
 *
 * @author TaskHub
 */
public enum UserStatus {

    /** 正常启用 */
    ACTIVE,

    /** 已停用（禁止登录，但历史数据保留） */
    DISABLED,

    /** 已锁定（如密码错误次数过多） */
    LOCKED
}
