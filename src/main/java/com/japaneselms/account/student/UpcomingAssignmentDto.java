package com.japaneselms.account.student;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Một bài tập sắp đến hạn hiển thị ở Student Dashboard.
 * Dùng Long (không dùng long) để constructor expression của JPQL khớp kiểu.
 */
public record UpcomingAssignmentDto(
        Long assignmentId,
        String title,
        Long courseId,
        String courseName,
        LocalDateTime dueAt) {

    /** Số ngày còn lại tính từ hôm nay (0 = hết hạn trong hôm nay). */
    public long daysLeft() {
        return ChronoUnit.DAYS.between(LocalDate.now(), dueAt.toLocalDate());
    }
}
