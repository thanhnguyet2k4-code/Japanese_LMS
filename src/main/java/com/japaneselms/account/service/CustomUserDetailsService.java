package com.japaneselms.account.service;

import com.japaneselms.entity.User;
import com.japaneselms.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email)
            throws UsernameNotFoundException {

        if (email == null || email.trim().isEmpty()) {
            throw new UsernameNotFoundException("Email không được để trống.");
        }

        String cleanEmail = email.trim();

        User user = userRepository.findByEmailIgnoreCase(cleanEmail)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Không tìm thấy tài khoản với email: " + cleanEmail
                ));

        if ("STUDENT".equalsIgnoreCase(user.getRole())
                && !"APPROVED".equalsIgnoreCase(user.getAccountStatus())) {
            throw new UsernameNotFoundException(
                    "Tài khoản sinh viên chưa được phê duyệt."
            );
        }

        if (!user.isActive()) {
            throw new UsernameNotFoundException("Tài khoản đã bị vô hiệu hóa.");
        }

        return user;
    }
}
