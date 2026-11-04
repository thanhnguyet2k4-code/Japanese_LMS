package com.japaneselms.repository;

import com.japaneselms.entity.Enrollment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    // UC07 & UC09: Kiểm tra sinh viên đã đăng ký khóa học này chưa
    boolean existsByStudent_UserIdAndCourse_CourseId(Long studentId, Long courseId);

    // UC10: Lấy danh sách khóa học đã đăng ký của Student (JOIN sẵn course và instructor để tránh N+1)
    @EntityGraph(attributePaths = {"course", "course.instructor"})
    Page<Enrollment> findByStudent_UserId(Long studentId, Pageable pageable);

    // UC08: Kiểm tra khóa học đã có sinh viên đăng ký chưa trước khi xóa/ngừng hoạt động
    boolean existsByCourse_CourseId(Long courseId);

    // Đếm số học viên đã đăng ký khóa học
    long countByCourse_CourseId(Long courseId);
}