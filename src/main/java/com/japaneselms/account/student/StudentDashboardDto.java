package com.japaneselms.account.student;

import com.japaneselms.course.dto.MyCourseDto;

import java.util.List;

/**
 * Toàn bộ dữ liệu của Student Dashboard (GET /student).
 */
public record StudentDashboardDto(
        String fullName,
        long enrolledCount,
        long activeCourseCount,
        long availableLessons,
        long pendingAssignments,
        long gradedCount,
        Double averageGrade,
        List<MyCourseDto> recentCourses,
        List<UpcomingAssignmentDto> upcomingAssignments) {
}
