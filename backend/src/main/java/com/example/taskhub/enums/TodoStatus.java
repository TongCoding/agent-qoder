package com.example.taskhub.enums;

import lombok.Getter;

/**
 * 待办事项状态。
 *
 * <p>典型流转路径：</p>
 * <pre>
 *   PENDING ──> IN_PROGRESS ──> DONE
 *      │              │
 *      └──────────────┴────────> CANCELLED
 * </pre>
 *
 * @author TaskHub
 */
@Getter
public enum TodoStatus {

    /** 待处理：已创建但尚未开始 */
    PENDING(false),

    /** 进行中：已开始处理但未完成 */
    IN_PROGRESS(false),

    /** 已完成：任务正常结束 */
    DONE(true),

    /** 已取消：任务被终止，不再处理 */
    CANCELLED(true);

    /**
     * 是否为终态（不会再发生变化）。
     *
     * <p>统计"未完成"数量、判断能否再次编辑时使用。</p>
     */
    private final boolean finished;

    TodoStatus(boolean finished) {
        this.finished = finished;
    }
}
