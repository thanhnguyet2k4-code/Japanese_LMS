
package com.japaneselms.config;

import com.japaneselms.entity.Course;
import com.japaneselms.entity.Enrollment;
import com.japaneselms.entity.User;
import com.japaneselms.repository.CourseRepository;
import com.japaneselms.repository.EnrollmentRepository;
import com.japaneselms.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AdminInitializer {

    @Bean
    @Order(1)
    public CommandLineRunner createTestUsers(
            UserRepository userRepository,
            CourseRepository courseRepository,
            EnrollmentRepository enrollmentRepository,
            PasswordEncoder passwordEncoder) {

        return args -> {
            // 1. Create the first system admin if it does not exist
            initUserIfAbsent(
                    userRepository,
                    passwordEncoder,
                    "admin@japaneselms.com",
                    "Admin@12345",
                    "System Administrator",
                    "ADMIN"
            );

            // 2. Create additional demo admin
            initUserIfAbsent(
                    userRepository,
                    passwordEncoder,
                    "admin@lms.com",
                    "123456",
                    "Admin Quan Tri Vien",
                    "ADMIN"
            );

            // 3. Create demo instructor
            User instructor = initUserIfAbsent(
                    userRepository,
                    passwordEncoder,
                    "takahashi@lms.com",
                    "123456",
                    "Thay Takahashi Kenji",
                    "INSTRUCTOR"
            );

            // 4. Create demo students
            User studentAn = initUserIfAbsent(
                    userRepository,
                    passwordEncoder,
                    "an.student@lms.com",
                    "123456",
                    "Nguyen Van An",
                    "STUDENT"
            );

            initUserIfAbsent(
                    userRepository,
                    passwordEncoder,
                    "cuong.student@lms.com",
                    "123456",
                    "Le Hoang Cuong",
                    "STUDENT"
            );

            // 5. Create a demo course and enrollment if no course exists
            if (courseRepository.count() == 0 && instructor != null) {
                Course course = new Course();
                course.setCourseName(
                        "Tieng Nhat So Cap N5 - Minna no Nihongo I"
                );
                course.setDescription(
                        "Khoa hoc tieng Nhat N5 co ban."
                );
                course.setJlptLevel("N5");
                course.setCourseStatus("ACTIVE");
                course.setInstructor(instructor);

                course = courseRepository.save(course);

                if (studentAn != null) {
                    Enrollment enrollment = new Enrollment();
                    enrollment.setCourse(course);
                    enrollment.setStudent(studentAn);
                    enrollmentRepository.save(enrollment);
                }
            }

            System.out.println(
                    ">>> Demo accounts and learning data initialization completed."
            );
        };
    }

    private User initUserIfAbsent(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            String email,
            String password,
            String fullName,
            String role) {

        return userRepository.findByEmailIgnoreCase(email)
                .orElseGet(() -> {
                    User user = new User();
                    user.setEmail(email.trim().toLowerCase());
                    user.setFullName(fullName);
                    user.setPasswordHash(passwordEncoder.encode(password));
                    user.setRole(role);
                    user.setAccountStatus("APPROVED");
                    user.setActive(true);

                    return userRepository.save(user);
                });
    }
}
