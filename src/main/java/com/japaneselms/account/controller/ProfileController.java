package com.japaneselms.account.controller;

import com.japaneselms.account.service.ProfileService;
import com.japaneselms.entity.User;
import com.japaneselms.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.japaneselms.account.dto.ProfileUpdateForm;
import com.japaneselms.account.dto.ChangePasswordForm;

@Controller
public class ProfileController {

    private final ProfileService profileService;
    private final UserRepository userRepository;

    public ProfileController(ProfileService profileService, UserRepository userRepository) {
        this.profileService = profileService;
        this.userRepository = userRepository;
    }

    // Hien thi ho so ca nhan.

    @GetMapping("/profile")
    public String showProfile(Authentication authentication, Model model) {
        User user = profileService.getProfile();
        model.addAttribute("user", user);

        // Khởi tạo form cập nhật hồ sơ
        ProfileUpdateForm profileForm = new ProfileUpdateForm();
        profileForm.setFullName(user.getFullName());
        profileForm.setPhone(user.getPhone());
        model.addAttribute("profileForm", profileForm);

        // Khởi tạo form đổi mật khẩu
        model.addAttribute("passwordForm", new ChangePasswordForm());

        boolean isAdmin = authentication != null
                && authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));

        if (isAdmin) {
            model.addAttribute("currentTab", "profile");

            long pendingCount = userRepository.findByAccountStatus("PENDING")
                    .stream()
                    .filter(u -> "STUDENT".equalsIgnoreCase(u.getRole()))
                    .count();

            model.addAttribute("unreadNotificationsCount", pendingCount);
            return "admin/profile";
        }

        model.addAttribute("currentTab", "profile");
        return "account/profile";
    }


    // Cap nhat ho ten va so dien thoai.

    @PostMapping("/profile")
    public String updateProfile(
            @Valid @ModelAttribute("profileForm") ProfileUpdateForm form,
            BindingResult bindingResult,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            return showProfileAfterError(authentication, model, form);
        }

        try {
            profileService.updateProfile(
                    form.getFullName(),
                    form.getPhone()
            );

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Cập nhật hồ sơ thành công."
            );

            return "redirect:/profile";

        } catch (IllegalArgumentException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            return showProfileAfterError(authentication, model, form);
        }
    }

    private String showProfileAfterError(
            Authentication authentication,
            Model model,
            ProfileUpdateForm form) {

        User user = profileService.getProfile();

        model.addAttribute("user", user);
        model.addAttribute("profileForm", form);
        model.addAttribute("passwordForm", new ChangePasswordForm());

        boolean isAdmin = authentication != null
                && authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));

        if (isAdmin) {
            model.addAttribute("currentTab", "profile");

            long pendingCount = userRepository.findByAccountStatus("PENDING")
                    .stream()
                    .filter(u -> "STUDENT".equalsIgnoreCase(u.getRole()))
                    .count();

            model.addAttribute("unreadNotificationsCount", pendingCount);
            return "admin/profile";
        }

        model.addAttribute("currentTab", "profile");
        return "account/profile";
    }


    // Xu ly doi mat khau.

    @PostMapping("/profile/change-password")
    public String changePassword(
            @Valid @ModelAttribute("passwordForm") ChangePasswordForm form,
            BindingResult bindingResult,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            return showProfileAfterPasswordError(authentication, model, form);
        }

        try {
            profileService.changePassword(
                    form.getCurrentPassword(),
                    form.getNewPassword(),
                    form.getConfirmPassword()
            );

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Đổi mật khẩu thành công."
            );

            return "redirect:/profile";

        } catch (IllegalArgumentException ex) {
            model.addAttribute("errorMessage", ex.getMessage());

            return showProfileAfterPasswordError(
                    authentication, model, new ChangePasswordForm()
            );
        }
    }

    private String showProfileAfterPasswordError(
            Authentication authentication,
            Model model,
            ChangePasswordForm passwordForm) {

        User user = profileService.getProfile();

        ProfileUpdateForm profileForm = new ProfileUpdateForm();
        profileForm.setFullName(user.getFullName());
        profileForm.setPhone(user.getPhone());

        model.addAttribute("user", user);
        model.addAttribute("profileForm", profileForm);
        model.addAttribute("passwordForm", passwordForm);

        boolean isAdmin = authentication != null
                && authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));

        if (isAdmin) {
            model.addAttribute("currentTab", "profile");

            long pendingCount = userRepository.findByAccountStatus("PENDING")
                    .stream()
                    .filter(u -> "STUDENT".equalsIgnoreCase(u.getRole()))
                    .count();

            model.addAttribute("unreadNotificationsCount", pendingCount);
            return "admin/profile";
        }

        model.addAttribute("currentTab", "profile");
        return "account/profile";
    }


}
