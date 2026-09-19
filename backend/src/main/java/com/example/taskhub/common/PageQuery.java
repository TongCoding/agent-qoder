package com.example.taskhub.common;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * 分页与排序查询参数基类。
 *
 * <p><b>注意</b>：对外暴露的 {@code page} 为 <b>1 起始</b>（与前端分页组件语义一致），
 * 内部会自动转换为 Spring Data 的 0 起始页码。</p>
 *
 * @author TaskHub
 */
@Data
@Schema(description = "分页查询基础参数")
public class PageQuery {

    /** 页码，从 1 开始 */
    @Min(value = 1, message = "页码必须大于 0")
    @Schema(description = "页码，从 1 开始", example = "1", defaultValue = "1")
    private Integer page = 1;

    /** 每页条数 */
    @Min(value = 1, message = "每页条数必须大于 0")
    @Max(value = 100, message = "每页条数不能超过 100")
    @Schema(description = "每页条数，最大 100", example = "10", defaultValue = "10")
    private Integer size = 10;

    /** 排序字段（需为白名单内的属性名，防止注入非法字段） */
    @Schema(description = "排序字段，为空时使用接口默认排序", example = "createdAt")
    private String sortBy;

    /** 排序方向：asc / desc */
    @Schema(description = "排序方向：asc 升序 / desc 降序", example = "desc", allowableValues = {"asc", "desc"})
    private String sortDir = "desc";

    /**
     * 构建 Spring Data 的 {@link Pageable} 对象。
     *
     * @param defaultSortBy   未指定排序字段时使用的默认字段
     * @param allowedSortKeys 允许排序的字段白名单（实体属性名）
     * @return 分页对象（0 起始页码）
     */
    public Pageable toPageable(String defaultSortBy, String... allowedSortKeys) {
        int pageIndex = (page == null || page < 1) ? 0 : page - 1;
        int pageSize = (size == null || size < 1) ? 10 : Math.min(size, 100);

        String field = resolveSortField(defaultSortBy, allowedSortKeys);
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;

        return PageRequest.of(pageIndex, pageSize, Sort.by(direction, field));
    }

    /**
     * 校验排序字段是否在白名单内，非法或未指定时回退到默认字段。
     */
    private String resolveSortField(String defaultSortBy, String... allowedSortKeys) {
        if (sortBy == null || sortBy.isBlank()) {
            return defaultSortBy;
        }
        for (String allowed : allowedSortKeys) {
            if (allowed.equalsIgnoreCase(sortBy)) {
                return allowed;
            }
        }
        // 未命中白名单，避免 PropertyReferenceException，回退默认排序
        return defaultSortBy;
    }
}
