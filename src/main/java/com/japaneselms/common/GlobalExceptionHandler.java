
package com.japaneselms.common;

import com.japaneselms.exception.AccessDeniedCourseException;
import com.japaneselms.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger =
            LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // Xu ly ResourceNotFoundException cua module Vu Tu
    @ExceptionHandler(com.japaneselms.exception.ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleCourseResourceNotFound(
            com.japaneselms.exception.ResourceNotFoundException ex,
            Model model) {

        model.addAttribute("errorCode", 404);
        model.addAttribute("errorMessage", ex.getMessage());
        model.addAttribute("message", ex.getMessage());

        return "error/404";
    }

    // Xu ly ResourceNotFoundException cua main
    @ExceptionHandler(com.japaneselms.common.exception.ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleResourceNotFound(
            com.japaneselms.common.exception.ResourceNotFoundException ex,
            Model model) {

        model.addAttribute("errorCode", 404);
        model.addAttribute("errorMessage", ex.getMessage());
        model.addAttribute("message", ex.getMessage());

        return "error/404";
    }

    // Xu ly loi khong co quyen truy cap khoa hoc cua Vu Tu
    @ExceptionHandler(AccessDeniedCourseException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String handleCourseAccessDenied(
            AccessDeniedCourseException ex,
            Model model) {

        model.addAttribute("errorCode", 403);
        model.addAttribute("errorMessage", ex.getMessage());
        model.addAttribute("message", ex.getMessage());

        return "error/403";
    }

    // Xu ly loi khong co quyen truy cap cua Spring Security
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String handleAccessDenied(Model model) {

        model.addAttribute("errorCode", 403);
        model.addAttribute("errorMessage", "Bạn không có quyền truy cập chức năng này.");
        model.addAttribute("message", "Bạn không có quyền truy cập chức năng này.");

        return "error/403";
    }

    // Xu ly loi nghiep vu cua Vu Tu
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleBusinessException(
            BusinessException ex,
            Model model) {

        model.addAttribute("errorCode", 400);
        model.addAttribute("errorMessage", ex.getMessage());
        model.addAttribute("message", ex.getMessage());

        return "error/500";
    }

    // Xu ly tep tai len vuot qua dung luong cho phep (cach xu ly cua Thang)
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleMaxSizeException(Model model) {

        String errorMessage = "Tệp tải lên vượt quá dung lượng cho phép (20MB).";

        model.addAttribute("errorCode", 400);
        model.addAttribute("errorMessage", errorMessage);
        model.addAttribute("message", errorMessage);

        return "error/500";
    }

    // Xu ly URL hoac tai nguyen khong ton tai
    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNoResource(Model model) {

        model.addAttribute("errorCode", 404);
        model.addAttribute("errorMessage", "Không tìm thấy tài nguyên.");
        model.addAttribute("message", "Không tìm thấy tài nguyên.");

        return "error/404";
    }

    // Xu ly cac loi khong duoc khai bao rieng
    @ExceptionHandler(Exception.class)
    public String handleException(Exception ex, Model model) {

        logger.error("Đã xảy ra lỗi khi xử lý yêu cầu.", ex);

        String errorMessage =
                "Hệ thống gặp sự cố. Vui lòng thử lại sau.";

        model.addAttribute("errorCode", 500);
        model.addAttribute("errorMessage", errorMessage);
        model.addAttribute("message", errorMessage);

        return "error/500";
    }
}
