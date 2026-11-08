
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
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
        model.addAttribute("errorMessage", "Ban khong co quyen truy cap.");
        model.addAttribute("message", "Ban khong co quyen truy cap.");

        return "error/403";
    }

    // Xu ly loi nghiep vu cua Vu Tu
    @ExceptionHandler(BusinessException.class)
    public String handleBusinessException(
            BusinessException ex,
            Model model) {

        model.addAttribute("errorCode", 400);
        model.addAttribute("errorMessage", ex.getMessage());
        model.addAttribute("message", ex.getMessage());

        return "error/500";
    }

    // Xu ly tep tai len vuot qua dung luong cho phep
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String handleMaxSizeException(
            MaxUploadSizeExceededException ex,
            RedirectAttributes redirectAttributes) {

        redirectAttributes.addFlashAttribute(
                "errorMessage",
                "Tep tai len vuot qua dung luong cho phep (20MB)."
        );

        return "redirect:/";
    }

    // Xu ly URL hoac tai nguyen khong ton tai
    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNoResource(Model model) {

        model.addAttribute("errorCode", 404);
        model.addAttribute("errorMessage", "Khong tim thay tai nguyen.");
        model.addAttribute("message", "Khong tim thay tai nguyen.");

        return "error/404";
    }

    // Xu ly cac loi khong duoc khai bao rieng
    @ExceptionHandler(Exception.class)
    public String handleException(Exception ex, Model model) {

        logger.error("Da xay ra loi khi xu ly yeu cau.", ex);

        String errorMessage =
                "He thong gap su co. Vui long thu lai sau.";

        model.addAttribute("errorCode", 500);
        model.addAttribute("errorMessage", errorMessage);
        model.addAttribute("message", errorMessage);

        return "error/500";
    }
}
