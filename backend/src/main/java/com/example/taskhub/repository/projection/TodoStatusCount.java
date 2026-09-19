package com.example.taskhub.repository.projection;

import com.example.taskhub.enums.TodoStatus;

/**
 * 按待办状态分组的计数投影。
 *
 * @author TaskHub
 */
public interface TodoStatusCount {

    /** 分组的状态值 */
    TodoStatus getStatus();

    /** 该状态下的待办数量 */
    Long getTotal();
}
