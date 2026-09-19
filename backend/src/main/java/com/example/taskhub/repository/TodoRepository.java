package com.example.taskhub.repository;

import com.example.taskhub.entity.TodoItem;
import com.example.taskhub.enums.TodoStatus;
import com.example.taskhub.repository.projection.AssigneeTodoCount;
import com.example.taskhub.repository.projection.TodoPriorityCount;
import com.example.taskhub.repository.projection.TodoStatusCount;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 待办事项数据访问接口。
 *
 * @author TaskHub
 */
@Repository
public interface TodoRepository extends JpaRepository<TodoItem, Long>, JpaSpecificationExecutor<TodoItem> {

    /**
     * 按 ID 查询待办，同时抓取负责人信息。
     *
     * <p>使用 {@code left join fetch} 一次性加载关联对象，避免详情页出现二次查询。</p>
     *
     * @param id 待办 ID
     */
    @Query("""
            select t from TodoItem t
            left join fetch t.assignee
            where t.id = :id
            """)
    Optional<TodoItem> findByIdWithAssignee(@Param("id") Long id);

    /**
     * 查询最近更新的待办（含负责人），用于仪表盘"最新动态"。
     *
     * @param pageable 仅使用其分页能力限制返回条数
     */
    @Query("""
            select t from TodoItem t
            left join fetch t.assignee
            order by t.updatedAt desc, t.id desc
            """)
    List<TodoItem> findRecentWithAssignee(Pageable pageable);

    /**
     * 是否存在指派给某用户的待办（删除用户前的关联校验）。
     */
    boolean existsByAssigneeId(Long assigneeId);

    /**
     * 统计指派给某用户的待办数量。
     */
    long countByAssigneeId(Long assigneeId);

    /**
     * 统计未分配负责人的待办数量。
     */
    long countByAssigneeIsNull();

    /**
     * 统计指定状态的待办数量。
     */
    long countByStatus(TodoStatus status);

    /**
     * 统计指定状态集合下的待办数量。
     */
    long countByStatusIn(Collection<TodoStatus> statuses);

    /**
     * 统计逾期未完成数量：截止日期早于给定日期，且状态处于未完成集合中。
     *
     * @param date     比较基准日期（通常为今天）
     * @param statuses 未完成状态集合
     */
    long countByDueDateBeforeAndStatusIn(LocalDate date, Collection<TodoStatus> statuses);

    /**
     * 统计指定日期到期且未完成的待办数量。
     *
     * @param date     目标日期
     * @param statuses 未完成状态集合
     */
    long countByDueDateAndStatusIn(LocalDate date, Collection<TodoStatus> statuses);

    /**
     * 按状态分组统计。
     */
    @Query("select t.status as status, count(t) as total from TodoItem t group by t.status")
    List<TodoStatusCount> countGroupByStatus();

    /**
     * 按优先级分组统计。
     */
    @Query("select t.priority as priority, count(t) as total from TodoItem t group by t.priority")
    List<TodoPriorityCount> countGroupByPriority();

    /**
     * 按负责人分组统计待办数量（一次聚合查询替代逐用户 count）。
     *
     * @param assigneeIds 负责人 ID 集合，不能为空
     */
    @Query("""
            select t.assignee.id as assigneeId, count(t) as total
            from TodoItem t
            where t.assignee.id in :assigneeIds
            group by t.assignee.id
            """)
    List<AssigneeTodoCount> countGroupByAssignee(@Param("assigneeIds") Collection<Long> assigneeIds);
}
