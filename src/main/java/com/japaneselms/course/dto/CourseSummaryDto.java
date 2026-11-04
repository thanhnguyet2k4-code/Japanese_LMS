package com.japaneselms.course.dto;

import com.japaneselms.entity.Course;

import java.time.LocalDateTime;

/**
 * Dữ liệu một thẻ khóa học ở S06 (danh sách công khai) và S09 (danh sách của Instructor).
 * Là DTO chỉ đọc nên dùng record; Thymeleaf truy cập bằng ${c.courseName}.
 * Phải gọi from(...) bên trong transaction vì Course.instructor là LAZY (open-in-view=false).
 */
public record CourseSummaryDto(
        Long courseId,
        String courseName,
        String description,
        String jlptLevel,
        String instructorName,
        String courseStatus,
        LocalDateTime createdAt) {

    public static CourseSummaryDto from(Course course) {
        return new CourseSummaryDto(
                course.getCourseId(),
                course.getCourseName(),
                course.getDescription(),
                course.getJlptLevel(),
                course.getInstructor().getFullName(),
                course.getCourseStatus(),
                course.getCreatedAt());
    }
}
