package com.japaneselms.repository;

import com.japaneselms.entity.Course;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Long> {
    // UC06: Tìm kiếm và lọc khóa học ACTIVE cho Guest/Student (Server-side Pagination & Filtering)
    @EntityGraph(attributePaths = {"instructor"})
    @Query("""
            SELECT c FROM Course c
            WHERE c.courseStatus = 'ACTIVE'
              AND (:keyword IS NULL OR :keyword = '' OR LOWER(c.courseName) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:jlptLevel IS NULL OR :jlptLevel = '' OR c.jlptLevel = :jlptLevel)
            """)
    Page<Course> searchActiveCourses(
            @Param("keyword") String keyword,
            @Param("jlptLevel") String jlptLevel,
            Pageable pageable
    );

    // UC07: Xem chi tiết 1 khóa học ACTIVE kèm thông tin Instructor (tránh LazyInitializationException)
    @EntityGraph(attributePaths = {"instructor"})
    Optional<Course> findByCourseIdAndCourseStatus(Long courseId, String courseStatus);

    // UC08: Lấy danh sách khóa học của đúng Instructor đang đăng nhập
    @EntityGraph(attributePaths = {"instructor"})
    Page<Course> findByInstructor_UserId(Long instructorId, Pageable pageable);

    // UC08: Kiểm tra quyền sở hữu khóa học của Instructor
    @EntityGraph(attributePaths = {"instructor"})
    Optional<Course> findByCourseIdAndInstructor_UserId(Long courseId, Long instructorId);

    // UC08: Kiểm tra trùng tên khóa học trong cùng 1 Instructor (khi tạo mới)
    boolean existsByInstructor_UserIdAndCourseNameIgnoreCase(Long instructorId, String courseName);

    // UC08: Kiểm tra trùng tên khóa học trong cùng 1 Instructor (khi cập nhật, trừ chính khóa học đó ra)
    boolean existsByInstructor_UserIdAndCourseNameIgnoreCaseAndCourseIdNot(
            Long instructorId, String courseName, Long courseId
    );
}
