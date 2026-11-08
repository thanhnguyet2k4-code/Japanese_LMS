package com.japaneselms.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger =
            LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * URL không tồn tại (không có controller / file tĩnh nào khớp) -> trả về 404.
     * Trước đây trường hợp này rơi vào handler Exception.class bên dưới và bị hiển thị thành lỗi 500.
     * Spring luôn chọn handler khớp loại exception cụ thể nhất nên không ảnh hưởng handler chung.
     */
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