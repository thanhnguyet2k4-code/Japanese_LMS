package com.japaneselms.course.controller;

import com.japaneselms.course.exception.CourseNotFoundException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Xử lý lỗi RIÊNG cho module Course (404 / 403).
 * <p>
 * Vì sao cần: GlobalExceptionHandler của Leader bắt @ExceptionHandler(Exception.class) nên sẽ nuốt
 * mọi lỗi và trả về trang 500. Advice này giới hạn trong package com.japaneselms.course và có
 *
 * @Order cao nhất để được ưu tiên trước, nhờ đó 404/403 hiển thị đúng mà KHÔNG phải sửa file của Leader.
 */
@ControllerAdvice(basePackages = "com.japaneselms.course")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CourseExceptionHandler {

    @ExceptionHandler(CourseNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNotFound() {
        return "error/404";
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String handleAccessDenied() {
        return "error/403";
    }
}
