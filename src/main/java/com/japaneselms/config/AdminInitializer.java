package com.japaneselms.config;

import com.japaneselms.entity.User;
import com.japaneselms.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Chỉ tạo tài khoản quản trị hệ thống đầu tiên khi chưa tồn tại,
 * để có thể đăng nhập và duyệt tài khoản. Không tạo dữ liệu mẫu.
 */
@Configuration
public class AdminInitializer {

    private static final String ADMIN_EMAIL = "admin@japaneselms.com";
    private static final String ADMIN_PASSWORD = "Admin@12345";

    @Bean
    @Order(1)
    public CommandLineRunner createSystemAdmin(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {

        return args -> {
            if (userRepository.findByEmailIgnoreCase(ADMIN_EMAIL).isPresent()) {
                return;
            }
            User admin = new User();
            admin.setEmail(ADMIN_EMAIL);
            admin.setFullName("System Administrator");
            admin.setPasswordHash(passwordEncoder.encode(ADMIN_PASSWORD));
            admin.setRole("ADMIN");
            admin.setAccountStatus("APPROVED");
            admin.setActive(true);
            userRepository.save(admin);
        };
    }
}
