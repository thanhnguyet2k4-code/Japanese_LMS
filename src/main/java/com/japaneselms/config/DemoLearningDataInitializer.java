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
public class DemoLearningDataInitializer {

    @Bean
    @Order(2)
    public CommandLineRunner createDemoLearningData(
            UserRepository userRepository,
            CourseRepository courseRepository,
            EnrollmentRepository enrollmentRepository,
            PasswordEncoder passwordEncoder) {

        return args -> {
            User instructor = userRepository
                    .findByEmailIgnoreCase("instructor@japaneselms.com")
                    .orElseGet(() -> {
                        User user = new User();
                        user.setFullName("Giảng viên Demo");
                        user.setEmail("instructor@japaneselms.com");
                        user.setPasswordHash(passwordEncoder.encode("Instructor@12345"));
                        user.setRole("INSTRUCTOR");
                        user.setAccountStatus("APPROVED");
                        user.setActive(true);
                        return userRepository.save(user);
                    });

            User student = userRepository
                    .findByEmailIgnoreCase("student@japaneselms.com")
                    .orElseGet(() -> {
                        User user = new User();
                        user.setFullName("Học viên Demo");
                        user.setEmail("student@japaneselms.com");
                        user.setPasswordHash(passwordEncoder.encode("Student@12345"));
                        user.setRole("STUDENT");
                        user.setAccountStatus("APPROVED");
                        user.setActive(true);
                        return userRepository.save(user);
                    });

            if (courseRepository.count() > 0) {
                return;
            }

            Course course = new Course();
            course.setCourseName("Tiếng Nhật sơ cấp N5");
            course.setDescription(
                    "Khóa học demo để quản lý bài học và tài liệu PDF, MP3, YouTube.");
            course.setJlptLevel("N5");
            course.setInstructor(instructor);
            course.setCourseStatus("ACTIVE");
            course = courseRepository.save(course);

            if (!enrollmentRepository.existsByStudent_UserIdAndCourse_CourseId(
                    student.getUserId(), course.getCourseId())) {
                Enrollment enrollment = new Enrollment();
                enrollment.setStudent(student);
                enrollment.setCourse(course);
                enrollmentRepository.save(enrollment);
            }

            System.out.println(
                    "Đã tạo dữ liệu demo: instructor@japaneselms.com / Instructor@12345 "
                            + "và student@japaneselms.com / Student@12345");
        };
    }
}
