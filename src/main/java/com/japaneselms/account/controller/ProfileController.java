
package com.japaneselms.account.controller;

import com.japaneselms.account.service.ProfileService;
import com.japaneselms.entity.User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    // Hien thi ho so ca nhan.
    @GetMapping("/profile")
    public String showProfile(Model model) {
        model.addAttribute("user", profileService.getProfile());
        return "account/profile";
    }

    // Cap nhat ho ten va so dien thoai.
    @PostMapping("/profile")
    public String updateProfile(
            @RequestParam String fullName,
            @RequestParam(required = false) String phone,
            RedirectAttributes redirectAttributes) {

        try {
            profileService.updateProfile(fullName, phone);
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Cap nhat ho so thanh cong.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage", ex.getMessage());
        }

        return "redirect:/profile";
    }

    // Xu ly doi mat khau.
    @PostMapping("/profile/change-password")
    public String changePassword(
            @RequestParam String currentPassword,
            @RequestParam String newPassword,
            @RequestParam String confirmPassword,
            RedirectAttributes redirectAttributes) {

        try {
            profileService.changePassword(
                    currentPassword,
                    newPassword,
                    confirmPassword);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Doi mat khau thanh cong.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage", ex.getMessage());
        }

        return "redirect:/profile";
    }
}

