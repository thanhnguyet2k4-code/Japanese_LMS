
package com.japaneselms.account.service;

import com.japaneselms.entity.User;
import com.japaneselms.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Optional;

@Service
public class AccountService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AccountService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // Chuan hoa email de tranh tao tai khoan trung.
    private String normalizeEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email khong duoc de trong.");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    // Chi cho phep tao tai khoan Instructor hoac Admin.
    @Transactional
    public User createAccount(
            String fullName,
            String email,
            String initialPassword,
            String role,
            String phone) {

        if (fullName == null || fullName.trim().isEmpty()) {
            throw new IllegalArgumentException("Ho ten khong duoc de trong.");
        }

        if (initialPassword == null || initialPassword.length() < 8) {
            throw new IllegalArgumentException(
                    "Mat khau ban dau phai co it nhat 8 ky tu.");
        }

        String normalizedEmail = normalizeEmail(email);

        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new IllegalArgumentException("Email da ton tai.");
        }

        String normalizedRole = role == null
                ? ""
                : role.trim().toUpperCase(Locale.ROOT);

        if (!normalizedRole.equals("INSTRUCTOR")
                && !normalizedRole.equals("ADMIN")) {
            throw new IllegalArgumentException(
                    "Chi duoc tao tai khoan INSTRUCTOR hoac ADMIN.");
        }

        User user = new User();
        user.setFullName(fullName.trim());
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(initialPassword));
        user.setRole(normalizedRole);
        user.setPhone(phone == null || phone.trim().isEmpty()
                ? null : phone.trim());

        // Tai khoan do Admin tao duoc phe duyet san.
        user.setAccountStatus("APPROVED");
        user.setActive(true);

        return userRepository.save(user);
    }

    // Chi cap nhat ho ten, email va so dien thoai.
    @Transactional
    public User updateAccount(
            Long userId,
            String fullName,
            String email,
            String phone) {

        User user = findAccount(userId);

        if (fullName == null || fullName.trim().isEmpty()) {
            throw new IllegalArgumentException("Ho ten khong duoc de trong.");
        }

        String normalizedEmail = normalizeEmail(email);

        Optional<User> existingUser =
                userRepository.findByEmailIgnoreCase(normalizedEmail);

        if (existingUser.isPresent()
                && !existingUser.get().getUserId().equals(userId)) {
            throw new IllegalArgumentException("Email da duoc tai khoan khac su dung.");
        }

        user.setFullName(fullName.trim());
        user.setEmail(normalizedEmail);
        user.setPhone(phone == null || phone.trim().isEmpty()
                ? null : phone.trim());

        // Khong thay doi role, active hoac accountStatus.
        return userRepository.save(user);
    }

    // Bat/tat tai khoan, khong xoa du lieu.
    @Transactional
    public User toggleAccountStatus(Long userId) {
        User user = findAccount(userId);

        // Khong cho phep Admin tu vo hieu hoa tai khoan hien tai.
        if (user.isActive() && isCurrentUser(user)) {
            throw new IllegalArgumentException(
                    "Ban khong the vo hieu hoa tai khoan cua chinh minh.");
        }

        // Khong cho phep vo hieu hoa Admin dang hoat dong cuoi cung.
        if (user.isActive()
                && isAdmin(user)
                && userRepository.countByRoleIgnoreCaseAndActiveTrue("ADMIN") <= 1) {
            throw new IllegalArgumentException(
                    "Khong the vo hieu hoa Admin dang hoat dong cuoi cung.");
        }

        user.setActive(!user.isActive());

        // Giu nguyen accountStatus (PENDING/APPROVED).
        return userRepository.save(user);
    }

    private User findAccount(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("Ma tai khoan khong hop le.");
        }

        return userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Khong tim thay tai khoan."));
    }

    private boolean isCurrentUser(User user) {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        return authentication != null
                && authentication.isAuthenticated()
                && user.getEmail().equalsIgnoreCase(authentication.getName());
    }

    private boolean isAdmin(User user) {
        return user.getRole() != null
                && user.getRole().equalsIgnoreCase("ADMIN");
    }
}
