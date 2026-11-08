package com.japaneselms.course.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Set;

/**
 * Làm sạch Pageable nhận từ request (page/size/sort do người dùng tự gõ được):
 * - page < 0           -> 0
 * - size < 1 hoặc > 50 -> mặc định 9 / tối đa 50 (BR5 của UC06, E3 của UC10)
 * - sort ngoài whitelist -> bỏ qua, dùng sort mặc định (chặn sort theo cột bất kỳ / lỗi PropertyReference)
 */
public final class PageableUtils {

    public static final int DEFAULT_SIZE = 9;
    public static final int MAX_SIZE = 50;

    private PageableUtils() {
    }

    public static Pageable sanitize(Pageable input, Set<String> allowedSortProperties, Sort defaultSort) {
        int page = Math.max(input.getPageNumber(), 0);

        int size = input.getPageSize();
        if (size < 1) {
            size = DEFAULT_SIZE;
        } else if (size > MAX_SIZE) {
            size = MAX_SIZE;
        }

        List<Sort.Order> allowedOrders = input.getSort().stream()
                .filter(order -> allowedSortProperties.contains(order.getProperty()))
                .toList();

        Sort sort = allowedOrders.isEmpty() ? defaultSort : Sort.by(allowedOrders);
        return PageRequest.of(page, size, sort);
    }
}
