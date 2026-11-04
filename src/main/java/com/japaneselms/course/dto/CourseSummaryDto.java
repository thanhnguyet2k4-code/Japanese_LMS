package com.japaneselms.course.dto;

import java.time.LocalDateTime;

public record CourseSummaryDto(
        Long courseId,
        String courseName,
        String description,
        String jlptLevel,
        String instructorName,
        String courseStatus,
        long studentCount,
        long lessonCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}