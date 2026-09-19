package com.example.taskhub.repository.projection;

import com.example.taskhub.enums.TodoPriority;

/**
 * 按待办优先级分组的计数投影。
 *
 * @author TaskHub
 */
public interface TodoPriorityCount {

    /** 分组的优先级值 */
    TodoPriority getPriority();

    /** 该优先级下的待办数量 */
    Long getTotal();
}
