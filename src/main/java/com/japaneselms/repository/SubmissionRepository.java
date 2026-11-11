
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

/**
 * Repository quan ly du lieu Submission.
 */
@Repository
public interface SubmissionRepository extends JpaRepository<Submission, Long> {

    // MF-04: Lay danh sach bai nop theo Assignment, sap xep theo thoi gian.
    @Query("SELECT s FROM Submission s JOIN FETCH s.student " +
            "WHERE s.assignment.assignmentId = :assignmentId " +
            "ORDER BY s.submittedAt ASC")
    List<Submission> findByAssignment_AssignmentIdOrderBySubmittedAtAsc(
            @Param("assignmentId") Long assignmentId
    );

    // MF-04: Tim bai nop cua Student trong Assignment.
    Optional<Submission> findByAssignment_AssignmentIdAndStudent_UserId(
            Long assignmentId,
            Long studentId
    );

    // MF-04: Kiem tra Student da nop bai hay chua.
    boolean existsByAssignment_AssignmentIdAndStudent_UserId(
            Long assignmentId,
            Long studentId
    );

    // MF-04: Dem so bai nop cua Assignment.
    long countByAssignment_AssignmentId(Long assignmentId);

    // MF-04: Lay toan bo bai nop cua Assignment (dung khi xoa Assignment).
    List<Submission> findByAssignment_AssignmentId(Long assignmentId);

    // MF-04: Lay danh sach bai nop co phan trang.
    Page<Submission> findByAssignment_AssignmentId(
            Long assignmentId,
            Pageable pageable
    );

    // MF-04: Loc bai nop theo trang thai cham diem.
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
