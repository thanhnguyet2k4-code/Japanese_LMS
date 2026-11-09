package com.japaneselms.grading.repository;

import com.japaneselms.entity.Submission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface GradingRepository extends JpaRepository<Submission, Long> {
    @EntityGraph(attributePaths = {"student", "assignment", "assignment.course", "gradedBy"})
    @Query("select s from Submission s where s.assignment.course.instructor.userId = :instructorId " +
           "and (:status = 'all' or (:status = 'pending' and s.grade is null) " +
           "or (:status = 'graded' and s.grade is not null))")
    Page<Submission> findForInstructor(@Param("instructorId") Long instructorId,
                                        @Param("status") String status, Pageable pageable);

    @EntityGraph(attributePaths = {"student", "assignment", "assignment.course", "gradedBy"})
    @Query("select s from Submission s where s.student.userId = :studentId and s.grade is not null")
    Page<Submission> findStudentGrades(@Param("studentId") Long studentId, Pageable pageable);

    @EntityGraph(attributePaths = {"student", "assignment", "assignment.course", "gradedBy"})
    @Query("select s from Submission s where s.assignment.course.courseId = :courseId")
    Page<Submission> findAdminResultsForCourse(@Param("courseId") Long courseId, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"student", "assignment", "assignment.course", "gradedBy"})
    Page<Submission> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"student", "assignment", "assignment.course", "gradedBy"})
    @Query("select s from Submission s where s.submissionId = :id")
    Optional<Submission> findDetailed(@Param("id") Long id);
}
