package com.japaneselms.repository;

import com.japaneselms.entity.Submission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubmissionRepository extends JpaRepository<Submission, Long> {

    @Query("SELECT s FROM Submission s JOIN FETCH s.student WHERE s.assignment.assignmentId = :assignmentId ORDER BY s.submittedAt ASC")
    List<Submission> findByAssignment_AssignmentIdOrderBySubmittedAtAsc(@Param("assignmentId") Long assignmentId);

    Optional<Submission> findByAssignment_AssignmentIdAndStudent_UserId(Long assignmentId, Long studentId);

    boolean existsByAssignment_AssignmentIdAndStudent_UserId(Long assignmentId, Long studentId);

    long countByAssignment_AssignmentId(Long assignmentId);

    Page<Submission> findByAssignment_AssignmentId(Long assignmentId, Pageable pageable);

    @Query("SELECT s FROM Submission s WHERE s.assignment.assignmentId = :assignmentId " +
           "AND (:gradingStatus IS NULL " +
           "     OR (:gradingStatus = 'GRADED' AND s.grade IS NOT NULL) " +
           "     OR (:gradingStatus = 'UNGRADED' AND s.grade IS NULL))")
    Page<Submission> findSubmissionsWithFilter(
            @Param("assignmentId") Long assignmentId,
            @Param("gradingStatus") String gradingStatus,
            Pageable pageable
    );
}
