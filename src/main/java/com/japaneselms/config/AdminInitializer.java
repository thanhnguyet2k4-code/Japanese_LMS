
package com.japaneselms.config;

import com.japaneselms.entity.User;
import com.japaneselms.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AdminInitializer {

    @Bean
    public CommandLineRunner createFirstAdmin(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {

        return args -> {
            String email = "admin@japaneselms.com";

            if (userRepository.existsByEmailIgnoreCase(email)) {
                System.out.println("Tài khoản admin đã tồn tại.");
                return;
            }

            User admin = new User();
            admin.setFullName("System Administrator");
            admin.setEmail(email);

            // Đổi mật khẩu này trước khi chạy trong môi trường dùng thật.
            admin.setPasswordHash(
                    passwordEncoder.encode("Admin@12345")
            );

            admin.setRole("ADMIN");
            admin.setAccountStatus("APPROVED");
            admin.setActive(true);

            userRepository.save(admin);

            System.out.println("Đã tạo tài khoản admin đầu tiên.");
        };
    }
}
