
package com.japaneselms.account.service;

import com.japaneselms.entity.User;
import com.japaneselms.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProfileService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public ProfileService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // Lay tai khoan cua nguoi dang dang nhap.
    private User getCurrentUser() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication.getName() == null) {
            throw new IllegalArgumentException(
                    "Không tìm thấy thông tin người đăng nhập.");
        }

        return userRepository.findByEmailIgnoreCase(
                        authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Không tìm thấy tài khoản."));
    }

    // Xem ho so ca nhan.
    @Transactional(readOnly = true)
    public User getProfile() {
        return getCurrentUser();
    }

    // Cap nhat ho ten va so dien thoai.
    @Transactional
    public User updateProfile(String fullName, String phone) {
        User user = getCurrentUser();

        if (fullName == null || fullName.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Họ tên không được để trống.");
        }

        if (fullName.trim().length() > 100) {
            throw new IllegalArgumentException(
                    "Họ tên không được vượt quá 100 ký tự.");
        }

        if (phone != null && phone.trim().length() > 20) {
            throw new IllegalArgumentException(
                    "Số điện thoại không được vượt quá 20 ký tự.");
        }

        user.setFullName(fullName.trim());
        user.setPhone(phone == null || phone.trim().isEmpty()
                ? null : phone.trim());

        return userRepository.save(user);
    }

    // Doi mat khau sau khi kiem tra mat khau hien tai.
    @Transactional
    public void changePassword(
            String currentPassword,
            String newPassword,
            String confirmPassword) {

        User user = getCurrentUser();

        if (currentPassword == null || currentPassword.isBlank()) {
            throw new IllegalArgumentException(
                    "Vui lòng nhập mật khẩu hiện tại.");
        }

        if (!passwordEncoder.matches(
                currentPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException(
                    "Mật khẩu hiện tại không chính xác.");
        }

        if (newPassword == null || newPassword.length() < 8) {
            throw new IllegalArgumentException(
                    "Mật khẩu mới phải có ít nhất 8 ký tự.");
        }

        if (confirmPassword == null
                || !newPassword.equals(confirmPassword)) {
            throw new IllegalArgumentException(
                    "Xác nhận mật khẩu không khớp.");
        }

        if (passwordEncoder.matches(
                newPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException(
                    "Mật khẩu mới phải khác mật khẩu hiện tại.");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}

