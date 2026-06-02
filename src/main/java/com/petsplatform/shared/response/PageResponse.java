package com.petsplatform.shared.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

/**
 * Page response wrapper cho các API trả về danh sách có phân trang.
 *
 * @param <T> Kiểu dữ liệu của items
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class PageResponse<T> {

    private final boolean success;
    private final String message;
    private final List<T> items;
    private final int page;
    private final int size;
    private final long totalElements;
    private final int totalPages;
    private final boolean hasNext;
    private final boolean hasPrevious;
    private final String timestamp;

    private PageResponse(
            boolean success,
            String message,
            List<T> items,
            int page,
            int size,
            long totalElements,
            int totalPages,
            boolean hasNext,
            boolean hasPrevious,
            String timestamp
    ) {
        this.success = success;
        this.message = message;
        this.items = items;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.hasNext = hasNext;
        this.hasPrevious = hasPrevious;
        this.timestamp = timestamp;
    }

    public static <T> PageResponse<T> of(
            List<T> items,
            int page,
            int size,
            long totalElements
    ) {
        int totalPages = (int) Math.ceil((double) totalElements / size);

        return PageResponse.<T>builder()
                .success(true)
                .message("Success")
                .items(items)
                .page(page)
                .size(size)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .hasNext(page < totalPages - 1)
                .hasPrevious(page > 0)
                .timestamp(Instant.now().toString())
                .build();
    }
}
