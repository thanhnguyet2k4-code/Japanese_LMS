
package com.japaneselms.repository;

import com.japaneselms.entity.Assignment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository quan ly du lieu Assignment.
 */
@Repository
public interface AssignmentRepository extends JpaRepository<Assignment, Long> {

    // MF-04: Lay Assignment kem Course va Lesson.
    @Query("SELECT a FROM Assignment a LEFT JOIN FETCH a.course LEFT JOIN FETCH a.lesson WHERE a.assignmentId = :id")
    Optional<Assignment> findByIdWithDetails(@Param("id") Long id);

    // MF-04: Lay danh sach Assignment theo Course co phan trang.
    Page<Assignment> findByCourse_CourseId(Long courseId, Pageable pageable);

    // MF-04: Loc Assignment theo trang thai.
    Page<Assignment> findByCourse_CourseIdAndAssignmentStatus(
            Long courseId,
            String status,
            Pageable pageable
    );

    // MF-04: Tim kiem Assignment theo tu khoa va trang thai.
    @Query("SELECT a FROM Assignment a WHERE a.course.courseId = :courseId " +
            "AND (:keyword IS NULL OR LOWER(a.title) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND (:status IS NULL OR a.assignmentStatus = :status)")
    Page<Assignment> searchAssignments(
            @Param("courseId") Long courseId,
            @Param("keyword") String keyword,
            @Param("status") String status,
            Pageable pageable
    );

    // MF-04: Lay tat ca Assignment theo Course.
    List<Assignment> findByCourse_CourseId(Long courseId);

    // MF-03: Kiem tra bai tap co thuoc bai hoc hay khong.
    boolean existsByLesson_LessonId(Long lessonId);

    // MF-02: Kiem tra khoa hoc da co bai tap hay chua.
    boolean existsByCourse_CourseId(Long courseId);
}
