package com.japaneselms.course.dto;

import java.time.LocalDateTime;

public record MyCourseDto(
        Long enrollmentId,
        Long courseId,
        String courseName,
        String description,
        String jlptLevel,
        String instructorName,
        String courseStatus,
        LocalDateTime enrolledAt
) {
}