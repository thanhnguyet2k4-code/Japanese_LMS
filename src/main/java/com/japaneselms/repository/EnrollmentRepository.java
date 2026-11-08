package com.japaneselms.repository;

import com.japaneselms.entity.Enrollment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository của bảng enrollments.
 * File gốc của Leader là class rỗng -> đổi thành interface (xem ghi chú ở CourseRepository).
 */
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    // ===================== MF-02 (Member 2) – BẮT ĐẦU =====================

    /**
     * UC07/UC09 – Student đã đăng ký khóa học này chưa? (chống đăng ký trùng)
     */
    boolean existsByStudent_UserIdAndCourse_CourseId(Long studentId, Long courseId);

    /**
     * UC08 – Khóa học đã có học viên đăng ký chưa? (quyết định Soft Delete)
     */
    boolean existsByCourse_CourseId(Long courseId);

    /**
     * UC10 – Các đăng ký của một Student, phân trang.
     *
     * @EntityGraph nạp luôn course và course.instructor trong 1 câu JOIN -> không bị N+1.
     */
    @EntityGraph(attributePaths = {"course", "course.instructor"})
    Page<Enrollment> findByStudent_UserId(Long studentId, Pageable pageable);

    // ====================== MF-02 (Member 2) – KẾT THÚC ======================
}
