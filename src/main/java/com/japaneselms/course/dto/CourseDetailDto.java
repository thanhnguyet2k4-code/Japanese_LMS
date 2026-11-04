package com.japaneselms.course.dto;

import java.time.LocalDateTime;

public record CourseDetailDto(
        Long courseId,
        String courseName,
        String description,
        String jlptLevel,
        String instructorName,
        String instructorEmail,
        String courseStatus,
        long lessonCount,
        long enrolledStudentCount,
        boolean enrolled,
        LocalDateTime createdAt
) {
}