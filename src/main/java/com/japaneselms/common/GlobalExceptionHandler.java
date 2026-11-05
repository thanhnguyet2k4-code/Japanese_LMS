package com.japaneselms.common;

import com.japaneselms.exception.AccessDeniedCourseException;
import com.japaneselms.exception.BusinessException;
import com.japaneselms.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public String handleResourceNotFound(ResourceNotFoundException ex, Model model) {
        model.addAttribute("errorCode", 404);
        model.addAttribute("errorMessage", ex.getMessage());
        return "error/404";
    }

    @ExceptionHandler(AccessDeniedCourseException.class)
    public String handleAccessDenied(AccessDeniedCourseException ex, Model model) {
        model.addAttribute("errorCode", 403);
        model.addAttribute("errorMessage", ex.getMessage());
        return "error/403";
    }

    @ExceptionHandler(BusinessException.class)
    public String handleBusinessException(BusinessException ex, Model model) {
        model.addAttribute("errorCode", 400);
        model.addAttribute("errorMessage", ex.getMessage());
        return "error/500";
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String handleMaxSizeException(MaxUploadSizeExceededException exc, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("errorMessage", "Dung lượng tệp vượt quá giới hạn tối đa cho phép (20MB).");
        return "redirect:/";
    }

    @ExceptionHandler(Exception.class)
    public String handleException(Exception exception, Model model) {
        logger.error("Đã xảy ra lỗi hệ thống:", exception);
        model.addAttribute("errorCode", 500);
        model.addAttribute("errorMessage", "Hệ thống gặp sự cố: " + exception.getMessage());
        return "error/500";
    }
}
