package com.japaneselms.course.controller;

import com.japaneselms.entity.User;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.Optional;

/**
 * Tiện ích đọc thông tin người dùng hiện tại từ Authentication (SecurityContext).
 * CustomUserDetailsService của Leader trả về chính Entity User làm principal,
 * nên lấy được userId mà không cần query thêm.
 * <p>
 * Tuyệt đối KHÔNG lấy studentId / instructorId từ @RequestParam hay form (chống IDOR).
 */
final class AuthUtils {

    private AuthUtils() {
    }

    /**
     * userId của người đang đăng nhập; rỗng nếu là Guest.
     */
    static Optional<Long> currentUserId(Authentication authentication) {
        if (authentication == null
                || authentication instanceof AnonymousAuthenticationToken
                || !authentication.isAuthenticated()) {
            return Optional.empty();
        }

        if (authentication.getPrincipal() instanceof User user) {
            return Optional.ofNullable(user.getUserId());
        }
        return Optional.empty();
    }

    /**
     * Giống currentUserId nhưng ném AccessDeniedException (-> 403) nếu không xác định được người dùng.
     */
    static Long requireUserId(Authentication authentication) {
        return currentUserId(authentication)
                .orElseThrow(() -> new AccessDeniedException("Không xác định được người dùng đăng nhập."));
    }

    /**
     * Người dùng hiện tại có vai trò (không kèm tiền tố ROLE_) này không?
     */
    static boolean hasRole(Authentication authentication, String role) {
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
            return false;
        }
        String authority = "ROLE_" + role;
        return authentication.getAuthorities().stream()
                .anyMatch(granted -> authority.equals(granted.getAuthority()));
    }
}
