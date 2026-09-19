package com.example.taskhub.enums;

/**
 * 用户角色。
 *
 * <p>本项目为纯 CRUD 示例，未接入 Spring Security，角色仅作为数据字段展示与筛选使用。</p>
 *
 * @author TaskHub
 */
public enum UserRole {

    /** 系统管理员：拥有全部数据的管理权限 */
    ADMIN,

    /** 项目经理：可管理任务与分配负责人 */
    MANAGER,

    /** 普通成员：仅处理分配给自己的任务 */
    MEMBER,

    /** 访客：只读权限 */
    GUEST
}
