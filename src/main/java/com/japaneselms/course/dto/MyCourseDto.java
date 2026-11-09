package com.japaneselms.course.dto;

import com.japaneselms.entity.Enrollment;

import java.time.LocalDateTime;

/**
 * Dữ liệu một khóa học đã đăng ký ở S08 – My Courses.
 */
public record MyCourseDto(
        Long enrollmentId,
        Long courseId,
        String courseName,
        String jlptLevel,
        String instructorName,
        String courseStatus,
        LocalDateTime enrolledAt,
        boolean hasCover,
        String coverVersion) {

    public static MyCourseDto from(Enrollment enrollment) {
        return new MyCourseDto(
                enrollment.getEnrollmentId(),
                enrollment.getCourse().getCourseId(),
                enrollment.getCourse().getCourseName(),
                enrollment.getCourse().getJlptLevel(),
                enrollment.getCourse().getInstructor().getFullName(),
                enrollment.getCourse().getCourseStatus(),
                enrollment.getEnrolledAt(),
                enrollment.getCourse().getCoverImagePath() != null
                        && !enrollment.getCourse().getCoverImagePath().isBlank(),
                CoverVersion.of(enrollment.getCourse().getUpdatedAt()));
    }
}
