package com.japaneselms.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http) throws Exception {

        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/",
                                "/login",
                                "/register",
                                "/403",
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/favicon.ico"
                        ).permitAll()

                        // ===== MF-02 (Member 2): Course & Enrollment =====
                        // S06, S07: Guest được xem danh sách và chi tiết khóa học (chỉ GET)
                        .requestMatchers(HttpMethod.GET, "/courses", "/courses/*").permitAll()
                        // UC09: chỉ STUDENT mới được đăng ký khóa học (POST, có CSRF)
                        .requestMatchers(HttpMethod.POST, "/courses/*/enroll").hasRole("STUDENT")
                        // S08 / UC10: chỉ STUDENT xem "Khóa học của tôi"
                        .requestMatchers("/my-courses", "/my-courses/**").hasRole("STUDENT")
                        // S09 / UC08: /instructor/courses/** đã nằm trong "/instructor/**" -> hasRole("INSTRUCTOR") bên dưới
                        // ===== hết phần MF-02 =====

                        .requestMatchers("/admin", "/admin/**").hasRole("ADMIN")
                        .requestMatchers("/student", "/student/**").hasRole("STUDENT")
                        .requestMatchers("/instructor", "/instructor/**")
                        .hasAnyRole("INSTRUCTOR", "ADMIN")
                        .requestMatchers("/files/materials/**").authenticated()
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .successHandler(roleBasedSuccessHandler())
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                )
                .exceptionHandling(exceptions -> exceptions
                .accessDeniedPage("/403")
        );

        return http.build();
    }

    @Bean
    public AuthenticationSuccessHandler roleBasedSuccessHandler() {
        return (request, response, authentication) -> {
            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(authority ->
                            authority.getAuthority().equals("ROLE_ADMIN"));

            boolean isStudent = authentication.getAuthorities().stream()
                    .anyMatch(authority ->
                            authority.getAuthority().equals("ROLE_STUDENT"));

            boolean isInstructor = authentication.getAuthorities().stream()
                    .anyMatch(authority ->
                            authority.getAuthority().equals("ROLE_INSTRUCTOR"));

            if (isAdmin) {
                response.sendRedirect("/admin");
            } else if (isStudent) {
                // MF-02: màn hình chính của Student là Student Dashboard
                response.sendRedirect("/student");
            } else if (isInstructor) {
                // MF-02: màn hình chính của Instructor là Instructor Dashboard
                response.sendRedirect("/instructor");
            } else {
                response.sendRedirect("/login?error");
            }
        };
    }
}