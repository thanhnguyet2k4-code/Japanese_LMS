package com.japaneselms.account.instructor;

/**
 * Một dòng "khóa học của tôi" ở Instructor Dashboard, kèm số học viên đã đăng ký.
 */
public record InstructorCourseRowDto(
        Long courseId,
        String courseName,
        String jlptLevel,
        String courseStatus,
        Long studentCount) {
}
