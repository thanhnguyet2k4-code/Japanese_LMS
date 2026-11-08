
package com.japaneselms.common;

import com.japaneselms.common.exception.ResourceNotFoundException;
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

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNotFound(ResourceNotFoundException exception, Model model) {
        model.addAttribute("message", exception.getMessage());
        return "error/404";
    }

    // Xu ly loi khong co quyen truy cap (403)
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String handleAccessDenied() {
        return "error/403";
    }

    // Xu ly tep tai len vuot qua dung luong cho phep
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleUploadTooLarge(Model model) {
        model.addAttribute(
                "message",
                "Tệp tải lên vượt quá dung lượng cho phép (20MB).");
        return "error/500";
    }

    // Xu ly URL hoac tai nguyen khong ton tai
    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNoResource() {
        return "error/404";
    }

    @ExceptionHandler(Exception.class)
    public String handleException(Exception exception, Model model) {

        logger.error("Đã xảy ra lỗi khi xử lý yêu cầu.", exception);

        model.addAttribute(
                "message",
                "Hệ thống gặp sự cố. Vui lòng thử lại sau."
        );

        return "error/500";
    }
}
