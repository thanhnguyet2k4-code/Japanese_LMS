
package com.japaneselms.account.controller;

import com.japaneselms.account.dto.RegisterRequest;
import com.japaneselms.account.service.RegistrationService;
import com.japaneselms.entity.User;
import com.japaneselms.repository.UserRepository;

import jakarta.validation.Valid;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class RegistrationController {

    private final RegistrationService registrationService;
    private final UserRepository userRepository;

    public RegistrationController(
            RegistrationService registrationService,
            UserRepository userRepository) {
        this.registrationService = registrationService;
        this.userRepository = userRepository;
    }

    // S01: Hien thi form dang ky hoc vien
    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        if (!model.containsAttribute("registerRequest")) {
            model.addAttribute("registerRequest", new RegisterRequest());
        }

        return "account/register";
    }

    // S01: Xu ly dang ky hoc vien
    @PostMapping("/register")
    public String registerStudent(
            @Valid @org.springframework.web.bind.annotation.ModelAttribute(
                    "registerRequest") RegisterRequest request,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        // Kiem tra cac truong trong DTO
        if (bindingResult.hasErrors()) {
            return "account/register";
        }

        // Kiem tra mat khau xac nhan
        if (!request.isPasswordMatching()) {
            bindingResult.rejectValue(
                    "confirmPassword",
                    "password.mismatch",
                    "Mật khẩu xác nhận không khớp."
            );
            return "account/register";
        }

        try {
            registrationService.registerStudent(request);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Đăng ký thành công! Vui lòng chờ Admin phê duyệt tài khoản."
            );

            return "redirect:/login";

        } catch (IllegalArgumentException e) {
            bindingResult.reject("registration.failed", e.getMessage());
            return "account/register";
        }
    }

    // S03: Danh sach hoc vien cho phe duyet
    @GetMapping("/admin/registrations")
    public String listPendingStudents(Model model) {
        model.addAttribute(
                "students",
                registrationService.getPendingStudents()
        );

        return "admin/registrations";
    }

    // S03: Phe duyet tai khoan hoc vien
    @PostMapping("/admin/registrations/{userId}/approve")
    public String approveStudent(
            @PathVariable Long userId,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        User admin = getCurrentAdmin(authentication);
        registrationService.approveStudent(userId, admin);

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Đã phê duyệt tài khoản học viên."
        );

        return "redirect:/admin/registrations";
    }

    // S03: Tu choi tai khoan hoc vien
    @PostMapping("/admin/registrations/{userId}/reject")
    public String rejectStudent(
            @PathVariable Long userId,
            @RequestParam(required = false) String reviewNote,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        User admin = getCurrentAdmin(authentication);

        registrationService.rejectStudent(
                userId,
                admin,
                reviewNote == null || reviewNote.isBlank()
                        ? "Tài khoản không được phê duyệt."
                        : reviewNote.trim()
        );

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Đã từ chối đăng ký học viên."
        );

        return "redirect:/admin/registrations";
    }

    // Tim tai khoan Admin hien tai
    private User getCurrentAdmin(Authentication authentication) {
        if (authentication == null) {
            throw new IllegalStateException(
                    "Không tìm thấy thông tin đăng nhập Admin."
            );
        }

        return userRepository.findByEmailIgnoreCase(authentication.getName())
                .filter(user ->
                        "ADMIN".equalsIgnoreCase(user.getRole()))
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Không tìm thấy tài khoản Admin."
                        ));
    }
}
