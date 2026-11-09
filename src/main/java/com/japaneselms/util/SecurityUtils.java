package com.japaneselms.util;

import com.japaneselms.entity.User;
import com.japaneselms.repository.UserRepository;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Lấy người dùng hiện tại từ Spring Security (principal là Entity User).
 * Không còn cơ chế giả lập đăng nhập qua session.
 */
@Component
public class SecurityUtils {

    private final UserRepository userRepository;

    public SecurityUtils(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return null;
        }
        if (auth.getPrincipal() instanceof User user) {
            return user;
        }
        return userRepository.findByEmailIgnoreCase(auth.getName()).orElse(null);
    }

    public Long getCurrentUserId() {
        User user = getCurrentUser();
        return user != null ? user.getUserId() : null;
    }
}
