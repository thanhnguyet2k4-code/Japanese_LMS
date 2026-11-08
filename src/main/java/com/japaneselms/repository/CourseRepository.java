
package com.japaneselms.repository;

import com.japaneselms.entity.Course;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Repository quan ly du lieu Course.
 */
public interface CourseRepository extends JpaRepository<Course, Long> {

    // ==================== MF-03: LESSON MANAGEMENT ====================

    /**
     * Lay danh sach khoa hoc cua giang vien, sap xep moi nhat truoc.
     */
    List<Course> findByInstructor_UserIdOrderByCreatedAtDesc(Long instructorId);

    /**
     * Tim khoa hoc kem thong tin Instructor theo courseId.
     */
    @Query("""
            select c from Course c
            join fetch c.instructor
            where c.courseId = :courseId
            """)
    Optional<Course> findWithInstructorById(@Param("courseId") Long courseId);


    // ==================== MF-02: COURSE MANAGEMENT ====================

    /**
     * UC06 - Tim kiem khoa hoc ACTIVE theo ten va cap do JLPT.
     */
    @EntityGraph(attributePaths = "instructor")
    @Query(value = """
            SELECT c FROM Course c
            WHERE c.courseStatus = 'ACTIVE'
              AND LOWER(c.courseName) LIKE :pattern ESCAPE '!'
              AND (:level = '' OR c.jlptLevel = :level)
            """,
            countQuery = """
                    SELECT COUNT(c) FROM Course c
                    WHERE c.courseStatus = 'ACTIVE'
                      AND LOWER(c.courseName) LIKE :pattern ESCAPE '!'
                      AND (:level = '' OR c.jlptLevel = :level)
                    """)
    Page<Course> searchActiveCourses(
            @Param("pattern") String pattern,
            @Param("level") String level,
            Pageable pageable);

    /**
     * UC07 - Tim khoa hoc theo id va trang thai, kem Instructor.
     */
    @EntityGraph(attributePaths = "instructor")
    Optional<Course> findByCourseIdAndCourseStatus(
            Long courseId, String courseStatus);

    /**
     * UC08 - Danh sach khoa hoc cua Instructor, co phan trang.
     */
    @EntityGraph(attributePaths = "instructor")
    Page<Course> findByInstructor_UserId(
            Long instructorId, Pageable pageable);

    /**
     * UC08 - Kiem tra trung ten khoa hoc khi tao moi.
     */
    boolean existsByInstructor_UserIdAndCourseNameIgnoreCase(
            Long instructorId, String courseName);

    /**
     * UC08 - Kiem tra trung ten khi cap nhat, loai tru khoa hoc hien tai.
     */
    boolean existsByInstructor_UserIdAndCourseNameIgnoreCaseAndCourseIdNot(
            Long instructorId, String courseName, Long courseId);
}
