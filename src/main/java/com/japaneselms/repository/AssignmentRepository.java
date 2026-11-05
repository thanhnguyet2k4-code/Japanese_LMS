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

@Repository
public interface AssignmentRepository extends JpaRepository<Assignment, Long> {

    @Query("SELECT a FROM Assignment a LEFT JOIN FETCH a.course LEFT JOIN FETCH a.lesson WHERE a.assignmentId = :id")
    Optional<Assignment> findByIdWithDetails(@Param("id") Long id);

    Page<Assignment> findByCourse_CourseId(Long courseId, Pageable pageable);

    Page<Assignment> findByCourse_CourseIdAndAssignmentStatus(Long courseId, String status, Pageable pageable);

    @Query("SELECT a FROM Assignment a WHERE a.course.courseId = :courseId " +
           "AND (:keyword IS NULL OR LOWER(a.title) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:status IS NULL OR a.assignmentStatus = :status)")
    Page<Assignment> searchAssignments(
            @Param("courseId") Long courseId,
            @Param("keyword") String keyword,
            @Param("status") String status,
            Pageable pageable
    );

    List<Assignment> findByCourse_CourseId(Long courseId);
}
