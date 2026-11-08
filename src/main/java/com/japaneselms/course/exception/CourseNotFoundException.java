package com.japaneselms.course.exception;

/**
 * Không tìm thấy khóa học (hoặc khóa học không ở trạng thái được phép xem) -> trang error/404.
 */
public class CourseNotFoundException extends RuntimeException {

    public CourseNotFoundException(Long courseId) {
        super("Không tìm thấy khóa học có mã " + courseId + ".");
    }
}
