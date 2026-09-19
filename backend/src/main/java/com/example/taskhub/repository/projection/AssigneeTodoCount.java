package com.example.taskhub.repository.projection;

/**
 * 按负责人分组的待办计数投影。
 *
 * <p>用于用户列表批量填充「名下待办数」，一次聚合查询替代逐行 count，避免 N+1。</p>
 *
 * @author TaskHub
 */
public interface AssigneeTodoCount {

    /** 负责人用户 ID */
    Long getAssigneeId();

    /** 该负责人名下的待办数量 */
    Long getTotal();
}
