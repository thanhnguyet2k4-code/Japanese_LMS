package com.japaneselms.account.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home(Model model, Authentication authentication) {

        // Phong ve: khong xu ly neu chua dang nhap
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {
            return "redirect:/login";
        }

        model.addAttribute("email", authentication.getName());
        model.addAttribute("role", authentication.getAuthorities());

        return "account/home";
    }
}