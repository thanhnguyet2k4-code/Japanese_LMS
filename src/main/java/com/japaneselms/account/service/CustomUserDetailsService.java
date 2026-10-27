
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

        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Không tìm thấy tài khoản với email: " + email
                ));

        if ("STUDENT".equalsIgnoreCase(user.getRole())
                && !"APPROVED".equalsIgnoreCase(user.getAccountStatus())) {
            throw new UsernameNotFoundException(
                    "Tài khoản sinh viên chưa được phê duyệt."
            );
        }

        return user;
    }
}
