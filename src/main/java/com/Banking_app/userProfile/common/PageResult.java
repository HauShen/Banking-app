package com.Banking_app.userProfile.common;

import java.util.List;
import java.util.function.Function;

public record PageResult<T>(List<T> content,
                            int page,
                            int size,
                            long totalElements) {

    public PageResult {
        content = List.copyOf(content);
        if (page < 0) {
            throw new IllegalArgumentException("page must not be negative");
        }
        if (size < 1) {
            throw new IllegalArgumentException("size must be at least 1");
        }
        if (totalElements < 0) {
            throw new IllegalArgumentException("totalElements must not be negative");
        }
    }

    public int totalPages() {
        return (int) Math.ceil((double) totalElements / size);
    }

    public boolean first() {
        return page == 0;
    }

    public boolean last() {
        return page >= totalPages() - 1;
    }

    public boolean empty() {
        return content.isEmpty();
    }

    /**
     * Converts the contents while keeping the paging metadata — this is what lets the web adapter
     * turn a {@code PageResult<UserProfile>} into a {@code PageResult<UserProfileResponseBody>}
     * without recomputing anything.
     */
    public <R> PageResult<R> map(Function<? super T, ? extends R> mapper) {
        List<R> mapped = content.stream().<R>map(mapper).toList();
        return new PageResult<>(mapped, page, size, totalElements);
    }
}