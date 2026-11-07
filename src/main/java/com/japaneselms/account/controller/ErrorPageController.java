package com.japaneselms.account.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ErrorPageController {

    @GetMapping("/403")
    public String forbidden() {
        return "error/403";
    }
}
