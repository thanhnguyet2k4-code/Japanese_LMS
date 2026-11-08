package com.japaneselms.account.instructor;

import java.time.LocalDateTime;

/**
 * Một lượt đăng ký gần đây vào khóa học của Instructor.
 */
public record RecentEnrollmentDto(
        String studentName,
        String studentEmail,
        String courseName,
        LocalDateTime enrolledAt) {
}
