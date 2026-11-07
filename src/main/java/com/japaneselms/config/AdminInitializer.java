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
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AdminInitializer {

    @Bean
    public CommandLineRunner createTestUsers(
            UserRepository userRepository,
            CourseRepository courseRepository,
            EnrollmentRepository enrollmentRepository,
            PasswordEncoder passwordEncoder) {

        return args -> {
            // 1. Quản trị viên (Admin)
            initUser(userRepository, passwordEncoder, "admin@japaneselms.com", "Admin@12345", "System Administrator", "ADMIN");
            initUser(userRepository, passwordEncoder, "admin@lms.com", "123456", "Admin Quản Trị Viên", "ADMIN");

            // 2. Giảng viên (Instructor)
            User instructor = initUser(userRepository, passwordEncoder, "takahashi@lms.com", "123456", "Thầy Takahashi Kenji", "INSTRUCTOR");

            // 3. Học viên (Student)
            User studentAn = initUser(userRepository, passwordEncoder, "an.student@lms.com", "123456", "Nguyễn Văn An", "STUDENT");
            initUser(userRepository, passwordEncoder, "cuong.student@lms.com", "123456", "Lê Hoàng Cường", "STUDENT");

            // 4. Đảm bảo có Course 1 do Giảng viên quản lý nếu chưa có
            if (courseRepository.count() == 0 && instructor != null) {
                Course course = new Course();
                course.setCourseName("Tiếng Nhật Sơ Cấp N5 - Minna no Nihongo I");
                course.setDescription("Khóa học tiếng Nhật N5 cơ bản.");
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

            System.out.println(">>> Đã khởi tạo và đồng bộ tài khoản hệ thống thành công.");
        };
    }

    private User initUser(UserRepository userRepository, PasswordEncoder passwordEncoder,
                          String email, String password, String fullName, String role) {
        User user = userRepository.findByEmailIgnoreCase(email).orElse(new User());
        user.setEmail(email.trim().toLowerCase());
        user.setFullName(fullName);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole(role);
        user.setAccountStatus("APPROVED");
        user.setActive(true);
        return userRepository.save(user);
    }
}
