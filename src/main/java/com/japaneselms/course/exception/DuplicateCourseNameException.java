package com.japaneselms.course.exception;

/**
 * Instructor đã có một khóa học trùng tên (UC08 – E3). Controller chuyển thành lỗi trên field courseName.
 */
public class DuplicateCourseNameException extends RuntimeException {

    public DuplicateCourseNameException() {
        super("Tên khóa học đã tồn tại.");
    }
}
