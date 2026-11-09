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
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
        boolean isInstructor = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_INSTRUCTOR".equals(authority.getAuthority()));
        boolean isStudent = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_STUDENT".equals(authority.getAuthority()));

        if (isAdmin) {
            return "redirect:/admin";
        }
        if (isInstructor) {
            return "redirect:/instructor";
        }
        if (isStudent) {
            return "redirect:/student";
        }
        model.addAttribute("email", authentication.getName());
        model.addAttribute("role", authentication.getAuthorities());

        return "account/home";
    }
}