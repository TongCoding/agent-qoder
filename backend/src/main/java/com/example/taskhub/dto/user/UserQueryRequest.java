package com.example.taskhub.dto.user;

import com.example.taskhub.common.PageQuery;
import com.example.taskhub.enums.UserRole;
import com.example.taskhub.enums.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户分页查询条件。
 *
 * <p>继承 {@link PageQuery} 获得分页与排序能力，所有筛选条件均为可选，
 * 为空时不参与过滤。</p>
 *
 * @author TaskHub
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "用户分页查询条件")
public class UserQueryRequest extends PageQuery {

    /** 关键字：模糊匹配用户名、昵称、邮箱 */
    @Schema(description = "关键字，模糊匹配用户名 / 昵称 / 邮箱", example = "zhang")
    private String keyword;

    /** 按角色筛选 */
    @Schema(description = "按角色筛选", example = "MEMBER")
    private UserRole role;

    /** 按账号状态筛选 */
    @Schema(description = "按账号状态筛选", example = "ACTIVE")
    private UserStatus status;

    /** 按部门精确筛选 */
    @Schema(description = "按部门筛选", example = "研发部")
    private String department;

    /** 用户列表允许排序的字段白名单 */
    public static final String[] SORTABLE_FIELDS = {"id", "username", "nickname", "status", "role", "createdAt", "updatedAt"};

    /** 默认排序字段 */
    public static final String DEFAULT_SORT_FIELD = "createdAt";
}
