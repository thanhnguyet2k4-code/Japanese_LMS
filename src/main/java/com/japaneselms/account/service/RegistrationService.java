
package com.japaneselms.account.service;

import com.japaneselms.account.dto.RegisterRequest;
import com.japaneselms.entity.User;
import com.japaneselms.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class RegistrationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public RegistrationService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void registerStudent(RegisterRequest request) {

        if (!request.isPasswordMatching()) {
            throw new IllegalArgumentException(
                    "Mật khẩu xác nhận không khớp.");
        }

        String email = request.getEmail()
                .trim()
                .toLowerCase(Locale.ROOT);

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException(
                    "Email này đã được sử dụng.");
        }

        User student = new User();
        student.setFullName(request.getFullName().trim());
        student.setEmail(email);
        student.setPhone(request.getPhoneNumber().trim());

        student.setPasswordHash(
                passwordEncoder.encode(request.getPassword()));

        student.setRole("STUDENT");
        student.setAccountStatus("PENDING");
        student.setActive(false);

        userRepository.save(student);
    }

    @Transactional(readOnly = true)
    public List<User> getPendingStudents() {
        return userRepository.findByAccountStatus("PENDING")
                .stream()
                .filter(user ->
                        "STUDENT".equalsIgnoreCase(user.getRole()))
                .toList();
    }

    @Transactional
    public void approveStudent(Long userId, User admin) {
        User student = findPendingStudent(userId);

        student.setAccountStatus("APPROVED");
        student.setReviewedBy(admin);
        student.setReviewedAt(LocalDateTime.now());
        student.setReviewNote(null);
        student.setActive(true);

        userRepository.save(student);
    }

    @Transactional
    public void rejectStudent(
            Long userId, User admin, String reviewNote) {
        User student = findPendingStudent(userId);

        student.setAccountStatus("REJECTED");
        student.setReviewedBy(admin);
        student.setReviewedAt(LocalDateTime.now());
        student.setReviewNote(reviewNote);
        student.setActive(false);

        userRepository.save(student);
    }

    private User findPendingStudent(Long userId) {
        User student = userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Không tìm thấy tài khoản học viên."));

        if (!"STUDENT".equalsIgnoreCase(student.getRole())
                || !"PENDING".equalsIgnoreCase(
                student.getAccountStatus())) {
            throw new IllegalStateException(
                    "Tài khoản không phải học viên đang chờ phê duyệt.");
        }

        return student;
    }
}
