package com.japaneselms.util;

import com.japaneselms.entity.User;
import com.japaneselms.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class SecurityUtils {

    private final UserRepository userRepository;

    public SecurityUtils(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User getCurrentUser() {
        // 1. Kiểm tra session giả lập test trước
        ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attr != null) {
            HttpServletRequest request = attr.getRequest();
            HttpSession session = request.getSession(false);
            if (session != null && session.getAttribute("testUserId") != null) {
                Long testUserId = (Long) session.getAttribute("testUserId");
                return userRepository.findById(testUserId).orElse(null);
            }
        }

        // 2. Kiểm tra Spring Security Principal của Member 1 khi tích hợp
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser")) {
            return userRepository.findByEmail(auth.getName()).orElse(null);
        }

        return null;
    }

    public Long getCurrentUserId() {
        User user = getCurrentUser();
        return user != null ? user.getUserId() : null;
    }
}
