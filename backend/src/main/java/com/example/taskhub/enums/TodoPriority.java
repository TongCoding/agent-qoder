package com.example.taskhub.enums;

import lombok.Getter;

/**
 * 待办事项优先级。
 *
 * <p>{@code weight} 为数值权重，可用于数据库排序或前端进度计算，数值越大越紧急。</p>
 *
 * @author TaskHub
 */
@Getter
public enum TodoPriority {

    /** 低：可延后处理 */
    LOW(1),

    /** 中：常规优先级（默认值） */
    MEDIUM(2),

    /** 高：需优先处理 */
    HIGH(3),

    /** 紧急：立即处理 */
    URGENT(4);

    /** 优先级权重，数值越大越紧急 */
    private final int weight;

    TodoPriority(int weight) {
        this.weight = weight;
    }
}
