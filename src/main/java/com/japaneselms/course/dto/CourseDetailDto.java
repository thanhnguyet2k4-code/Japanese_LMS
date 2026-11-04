package com.japaneselms.course.dto;

import com.japaneselms.entity.Course;

import java.time.LocalDateTime;

/**
 * Dữ liệu màn hình S07 – chi tiết khóa học. Không chứa nội dung tài liệu bài học (thuộc MF-03).
 */
public record CourseDetailDto(
        Long courseId,
        String courseName,
        String description,
        String jlptLevel,
        String instructorName,
        String courseStatus,
        long lessonCount,
        LocalDateTime createdAt) {

    public static CourseDetailDto from(Course course, long lessonCount) {
        return new CourseDetailDto(
                course.getCourseId(),
                course.getCourseName(),
                course.getDescription(),
                course.getJlptLevel(),
                course.getInstructor().getFullName(),
                course.getCourseStatus(),
                lessonCount,
                course.getCreatedAt());
    }
}
