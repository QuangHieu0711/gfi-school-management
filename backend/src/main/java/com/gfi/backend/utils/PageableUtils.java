package com.gfi.backend.utils;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public final class PageableUtils {

    private static final Sort DEFAULT_NEWEST_FIRST_SORT = Sort.by(Sort.Direction.DESC, "createdAt")
            .and(Sort.by(Sort.Direction.DESC, "id"));

    private PageableUtils() {
    }

    public static Pageable newestFirst(int pageNow, int pageSize) {
        return PageRequest.of(pageNow - 1, pageSize, DEFAULT_NEWEST_FIRST_SORT);
    }
}
