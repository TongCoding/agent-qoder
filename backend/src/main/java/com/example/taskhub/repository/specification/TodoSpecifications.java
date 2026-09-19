package com.example.taskhub.repository.specification;

import com.example.taskhub.dto.todo.TodoQueryRequest;
import com.example.taskhub.entity.TodoItem;
import com.example.taskhub.enums.TodoStatus;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 待办事项动态查询条件构造器。
 *
 * <p>基于 JPA Criteria API 实现「条件可选」的组合查询：查询参数为空时自动跳过该条件，
 * 相比拼接 JPQL 字符串更安全（不存在注入风险），也避免了 {@code :param is null or ...}
 * 这类写法在部分数据库上无法命中索引的问题。</p>
 *
 * @author TaskHub
 */
public final class TodoSpecifications {

    /** 负责人关联属性名 */
    private static final String ASSIGNEE = "assignee";

    private static final String TITLE = "title";
    private static final String DESCRIPTION = "description";
    private static final String STATUS = "status";
    private static final String PRIORITY = "priority";
    private static final String DUE_DATE = "dueDate";

    /** 未完成状态集合，用于逾期判断 */
    private static final List<TodoStatus> UNFINISHED_STATUSES = Arrays.stream(TodoStatus.values())
            .filter(status -> !status.isFinished())
            .toList();

    /** 已完成状态集合 */
    private static final List<TodoStatus> FINISHED_STATUSES = Arrays.stream(TodoStatus.values())
            .filter(TodoStatus::isFinished)
            .toList();

    private TodoSpecifications() {
        // 工具类，禁止实例化
    }

    /**
     * 根据查询请求构建 Specification。
     *
     * @param query 查询条件，可为 null（表示不加任何过滤）
     * @return 可组合的查询规格
     */
    public static Specification<TodoItem> filter(TodoQueryRequest query) {
        return (root, criteriaQuery, cb) -> {
            // 列表查询抓取负责人，规避 N+1；count 查询必须跳过 fetch
            fetchAssigneeForListQuery(root, criteriaQuery);

            if (query == null) {
                return cb.conjunction();
            }

            List<Predicate> predicates = new ArrayList<>();

            // ---------- 关键字：标题 / 描述模糊匹配 ----------
            if (StringUtils.hasText(query.getKeyword())) {
                String pattern = "%" + query.getKeyword().trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get(TITLE)), pattern),
                        cb.like(cb.lower(root.get(DESCRIPTION)), pattern)
                ));
            }

            // ---------- 精确匹配条件 ----------
            if (query.getStatus() != null) {
                predicates.add(cb.equal(root.get(STATUS), query.getStatus()));
            }
            if (query.getPriority() != null) {
                predicates.add(cb.equal(root.get(PRIORITY), query.getPriority()));
            }
            if (query.getAssigneeId() != null) {
                predicates.add(cb.equal(root.get(ASSIGNEE).get("id"), query.getAssigneeId()));
            }
            if (Boolean.TRUE.equals(query.getUnassigned())) {
                predicates.add(cb.isNull(root.get(ASSIGNEE)));
            }

            // ---------- 截止日期区间 ----------
            Path<LocalDate> dueDate = root.get(DUE_DATE);
            if (query.getDueDateFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(dueDate, query.getDueDateFrom()));
            }
            if (query.getDueDateTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(dueDate, query.getDueDateTo()));
            }

            // ---------- 逾期筛选 ----------
            LocalDate today = LocalDate.now();
            if (Boolean.TRUE.equals(query.getOverdue())) {
                predicates.add(cb.and(
                        cb.isNotNull(dueDate),
                        cb.lessThan(dueDate, today),
                        root.get(STATUS).in(UNFINISHED_STATUSES)
                ));
            } else if (Boolean.FALSE.equals(query.getOverdue())) {
                // 未逾期 = 没有截止日期 或 截止日期未过 或 任务已进入终态
                predicates.add(cb.or(
                        cb.isNull(dueDate),
                        cb.greaterThanOrEqualTo(dueDate, today),
                        root.get(STATUS).in(FINISHED_STATUSES)
                ));
            }

            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * 仅在列表查询中抓取负责人关联。
     *
     * <p>Spring Data 在执行分页查询时会用同一个 Specification 额外生成一条 count 语句，
     * 若 count 语句里带有 {@code fetch join}，Hibernate 会抛出
     * 「query specified join fetching, but the owner of the fetched association was not present
     * in the select list」，因此需要按结果类型跳过。</p>
     */
    private static void fetchAssigneeForListQuery(Root<TodoItem> root, CriteriaQuery<?> criteriaQuery) {
        if (criteriaQuery == null) {
            return;
        }
        Class<?> resultType = criteriaQuery.getResultType();
        if (Long.class.equals(resultType) || long.class.equals(resultType)) {
            return;
        }
        boolean alreadyFetched = root.getFetches().stream()
                .anyMatch(fetch -> ASSIGNEE.equals(fetch.getAttribute().getName()));
        if (!alreadyFetched) {
            root.fetch(ASSIGNEE, JoinType.LEFT);
        }
    }
}
