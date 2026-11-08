
package com.japaneselms.repository;

import com.japaneselms.entity.Enrollment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Repository quan ly du lieu Enrollment.
 */
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    // ==================== MF-03: LESSON MANAGEMENT ====================

    /**
     * Xoa tat ca dang ky cua mot khoa hoc.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from Enrollment e where e.course.courseId = :courseId")
    void deleteAllByCourseId(@Param("courseId") Long courseId);

    /**
     * Lay danh sach khoa hoc da dang ky cua Student,
     * kem Course va Instructor, moi nhat truoc.
     */
    @Query("""
            select e from Enrollment e
            join fetch e.course c
            join fetch c.instructor
            where e.student.userId = :studentId
            order by e.enrolledAt desc
            """)
    List<Enrollment> findWithCourseByStudentId(
            @Param("studentId") Long studentId);


    // ==================== MF-02: COURSE MANAGEMENT ====================

    /**
     * UC07/UC09 - Kiem tra Student da dang ky khoa hoc chua.
     */
    boolean existsByStudent_UserIdAndCourse_CourseId(
            Long studentId, Long courseId);

    /**
     * UC08 - Kiem tra khoa hoc da co hoc vien dang ky chua.
     */
    boolean existsByCourse_CourseId(Long courseId);

    /**
     * UC10 - Lay danh sach dang ky cua Student co phan trang.
     * EntityGraph nap Course va Instructor de tranh N+1.
     */
    @EntityGraph(attributePaths = {"course", "course.instructor"})
    Page<Enrollment> findByStudent_UserId(
            Long studentId, Pageable pageable);
}
