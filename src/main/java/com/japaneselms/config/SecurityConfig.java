package com.japaneselms.config;

import com.japaneselms.account.service.CustomUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

@Configuration
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;

    public SecurityConfig(
            CustomUserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder) {

        this.userDetailsService = userDetailsService;
        this.passwordEncoder = passwordEncoder;
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {

        DaoAuthenticationProvider authProvider =
                new DaoAuthenticationProvider();

        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder);

        return authProvider;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http) throws Exception {

        http
                .authenticationProvider(authenticationProvider())

                .authorizeHttpRequests(auth -> auth

                        // Public pages and static resources
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

                        // Anh bia khoa hoc hien tren trang cong khai (GET only)
                        .requestMatchers(
                                HttpMethod.GET,
                                "/files/course-images/*"
                        ).permitAll()

                        // MF-02: Public course listing and detail (GET only)
                        .requestMatchers(
                                HttpMethod.GET,
                                "/courses",
                                "/courses/*"
                        ).permitAll()

                        // MF-02: Only Student can enroll in a course
                        .requestMatchers(
                                HttpMethod.POST,
                                "/courses/*/enroll"
                        ).hasRole("STUDENT")

                        // Student course management
                        .requestMatchers(
                                "/my-courses",
                                "/my-courses/**"
                        ).hasRole("STUDENT")

                        // MF-05: S22 & S23 - Specific rules for Admin assessment results (GET only)
                        .requestMatchers(
                                HttpMethod.GET,
                                "/admin/assessment-results",
                                "/admin/assessment-results/**",
                                "/admin/results",
                                "/admin/results/**"
                        ).hasRole("ADMIN")

                        // MF-05: S22 & S23 - Specific rules for Instructor assessment results (GET only)
                        .requestMatchers(
                                HttpMethod.GET,
                                "/instructor/assessment-results",
                                "/instructor/assessment-results/**",
                                "/instructor/results",
                                "/instructor/results/**"
                        ).hasRole("INSTRUCTOR")

                        // Admin pages
                        .requestMatchers(
                                "/admin",
                                "/admin/**"
                        ).hasRole("ADMIN")

                        // Student pages
                        .requestMatchers(
                                "/student",
                                "/student/**"
                        ).hasRole("STUDENT")

                        // Instructor pages, including Assignment and Grading
                        .requestMatchers(
                                "/instructor",
                                "/instructor/**"
                        ).hasAnyRole("INSTRUCTOR", "ADMIN")

                        // Learning materials require authentication
                        .requestMatchers(
                                "/files/materials/**"
                        ).authenticated()

                        // All other pages require authentication
                        .anyRequest().authenticated()
                )

                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .usernameParameter("username")
                        .passwordParameter("password")
                        .successHandler(roleBasedSuccessHandler())
                        .failureHandler((request, response, exception) -> {
                            response.sendRedirect("/login?error");
                        })
                        .permitAll()
                )

                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
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
                response.sendRedirect("/student");

            } else if (isInstructor) {
                response.sendRedirect("/instructor");

            } else {
                response.sendRedirect("/login?error");
            }
        };
    }
}
