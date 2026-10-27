
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
                    "Khong tim thay thong tin nguoi dang nhap.");
        }

        return userRepository.findByEmailIgnoreCase(
                        authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Khong tim thay tai khoan."));
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
                    "Ho ten khong duoc de trong.");
        }

        if (fullName.trim().length() > 100) {
            throw new IllegalArgumentException(
                    "Ho ten khong duoc vuot qua 100 ky tu.");
        }

        if (phone != null && phone.trim().length() > 20) {
            throw new IllegalArgumentException(
                    "So dien thoai khong duoc vuot qua 20 ky tu.");
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
                    "Vui long nhap mat khau hien tai.");
        }

        if (!passwordEncoder.matches(
                currentPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException(
                    "Mat khau hien tai khong chinh xac.");
        }

        if (newPassword == null || newPassword.length() < 8) {
            throw new IllegalArgumentException(
                    "Mat khau moi phai co it nhat 8 ky tu.");
        }

        if (confirmPassword == null
                || !newPassword.equals(confirmPassword)) {
            throw new IllegalArgumentException(
                    "Xac nhan mat khau khong khop.");
        }

        if (passwordEncoder.matches(
                newPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException(
                    "Mat khau moi phai khac mat khau hien tai.");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}

