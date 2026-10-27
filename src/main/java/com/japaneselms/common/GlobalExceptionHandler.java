
package com.japaneselms.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger =
            LoggerFactory.getLogger(GlobalExceptionHandler.class);

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
