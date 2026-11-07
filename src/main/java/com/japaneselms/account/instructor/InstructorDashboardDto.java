package com.japaneselms.account.instructor;

import java.util.List;

/**
 * Toàn bộ dữ liệu của Instructor Dashboard (GET /instructor).
 */
public record InstructorDashboardDto(
        String fullName,
        long totalCourses,
        long activeCourses,
        long totalStudents,
        long pendingSubmissions,
        List<InstructorCourseRowDto> recentCourses,
        List<RecentEnrollmentDto> recentEnrollments) {
}
