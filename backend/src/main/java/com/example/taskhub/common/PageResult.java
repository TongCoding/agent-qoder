package com.example.taskhub.common;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * 统一分页结果封装。
 *
 * <p>对 Spring Data 的 {@link Page} 做瘦身，只暴露前端真正需要的字段，
 * 避免序列化 {@code Pageable}、{@code Sort} 等冗余信息。</p>
 *
 * <p>页码语义与 {@link PageQuery} 保持一致，均为 <b>1 起始</b>。</p>
 *
 * @param content      当前页数据列表
 * @param page         当前页码（从 1 开始）
 * @param size         每页条数
 * @param totalElements 总记录数
 * @param totalPages   总页数
 * @param first        是否为首页
 * @param last         是否为末页
 * @param empty        当前页是否为空
 * @param <T>          数据类型
 * @author TaskHub
 */
@Schema(description = "分页结果")
public record PageResult<T>(
        @Schema(description = "当前页数据列表") List<T> content,
        @Schema(description = "当前页码，从 1 开始", example = "1") int page,
        @Schema(description = "每页条数", example = "10") int size,
        @Schema(description = "总记录数", example = "42") long totalElements,
        @Schema(description = "总页数", example = "5") int totalPages,
        @Schema(description = "是否为首页") boolean first,
        @Schema(description = "是否为末页") boolean last,
        @Schema(description = "当前页是否为空") boolean empty
) {

    /**
     * 由 Spring Data 的 {@link Page} 直接转换（元素类型不变）。
     *
     * @param source 分页查询结果
     */
    public static <T> PageResult<T> from(Page<T> source) {
        return new PageResult<>(
                source.getContent(),
                source.getNumber() + 1,
                source.getSize(),
                source.getTotalElements(),
                source.getTotalPages(),
                source.isFirst(),
                source.isLast(),
                source.isEmpty()
        );
    }

    /**
     * 由 Spring Data 的 {@link Page} 转换，并将实体映射为 DTO。
     *
     * @param source    实体分页结果
     * @param converter 实体 -> DTO 的转换函数
     */
    public static <E, T> PageResult<T> from(Page<E> source, Function<E, T> converter) {
        return from(source.map(converter));
    }

    /**
     * 由 Spring Data 的 {@link Page} 转换，使用调用方已经批量准备好的 DTO 列表。
     *
     * <p>适用于需要先做一次批量聚合查询（如统计每个用户的待办数）再组装 DTO 的场景，
     * 可以避免在转换函数中逐行查库导致的 N+1 问题。</p>
     *
     * @param source  实体分页结果，仅提供分页元信息
     * @param content 与 {@code source.getContent()} 顺序一致的 DTO 列表
     */
    public static <E, T> PageResult<T> from(Page<E> source, List<T> content) {
        return new PageResult<>(
                content,
                source.getNumber() + 1,
                source.getSize(),
                source.getTotalElements(),
                source.getTotalPages(),
                source.isFirst(),
                source.isLast(),
                content.isEmpty()
        );
    }

    /**
     * 构造一个空的分页结果。
     *
     * @param page 页码（从 1 开始）
     * @param size 每页条数
     */
    public static <T> PageResult<T> empty(int page, int size) {
        return new PageResult<>(List.of(), page, size, 0L, 0, true, true, true);
    }
}
