package com.example.taskhub.repository.specification;

import com.example.taskhub.dto.user.UserQueryRequest;
import com.example.taskhub.entity.User;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 用户动态查询条件构造器。
 *
 * @author TaskHub
 */
public final class UserSpecifications {

    private static final String USERNAME = "username";
    private static final String NICKNAME = "nickname";
    private static final String EMAIL = "email";
    private static final String DEPARTMENT = "department";
    private static final String ROLE = "role";
    private static final String STATUS = "status";

    private UserSpecifications() {
        // 工具类，禁止实例化
    }

    /**
     * 根据查询请求构建 Specification。
     *
     * @param query 查询条件，可为 null（表示不加任何过滤）
     * @return 可组合的查询规格
     */
    public static Specification<User> filter(UserQueryRequest query) {
        return (root, criteriaQuery, cb) -> {
            if (query == null) {
                return cb.conjunction();
            }

            List<Predicate> predicates = new ArrayList<>();

            // ---------- 关键字：用户名 / 昵称 / 邮箱模糊匹配 ----------
            if (StringUtils.hasText(query.getKeyword())) {
                String pattern = "%" + query.getKeyword().trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get(USERNAME)), pattern),
                        cb.like(cb.lower(root.get(NICKNAME)), pattern),
                        cb.like(cb.lower(root.get(EMAIL)), pattern)
                ));
            }

            if (query.getRole() != null) {
                predicates.add(cb.equal(root.get(ROLE), query.getRole()));
            }
            if (query.getStatus() != null) {
                predicates.add(cb.equal(root.get(STATUS), query.getStatus()));
            }
            if (StringUtils.hasText(query.getDepartment())) {
                predicates.add(cb.equal(root.get(DEPARTMENT), query.getDepartment().trim()));
            }

            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
