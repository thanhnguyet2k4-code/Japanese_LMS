package com.japaneselms.repository;

import com.japaneselms.account.student.UpcomingAssignmentDto;
import com.japaneselms.entity.Enrollment;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Các truy vấn thống kê cho Student Dashboard.
 * File MỚI, tách riêng để không sửa các Repository của Leader / Member khác (tránh conflict khi merge).
 */
public interface StudentDashboardRepository extends Repository<Enrollment, Long> {

    @Query("SELECT COUNT(e) FROM Enrollment e WHERE e.student.userId = :studentId")
    long countEnrollments(@Param("studentId") Long studentId);

    @Query("""
            SELECT COUNT(e) FROM Enrollment e
            WHERE e.student.userId = :studentId
              AND e.course.courseStatus = 'ACTIVE'
            """)
    long countActiveCourses(@Param("studentId") Long studentId);

    /**
     * Bài học ACTIVE thuộc các khóa học ACTIVE mà student đã đăng ký.
     */
    @Query("""
            SELECT COUNT(l) FROM Lesson l
            WHERE l.lessonStatus = 'ACTIVE'
              AND l.course.courseStatus = 'ACTIVE'
              AND l.course.courseId IN (
                  SELECT e.course.courseId FROM Enrollment e WHERE e.student.userId = :studentId)
            """)
    long countAvailableLessons(@Param("studentId") Long studentId);

    /**
     * Bài tập (đang mở) của các khóa học ACTIVE đã đăng ký mà student CHƯA nộp.
     */
    @Query("""
            SELECT COUNT(a) FROM Assignment a
            WHERE a.assignmentStatus = :status
              AND a.course.courseStatus = 'ACTIVE'
              AND a.course.courseId IN (
                  SELECT e.course.courseId FROM Enrollment e WHERE e.student.userId = :studentId)
              AND NOT EXISTS (
                  SELECT s.submissionId FROM Submission s
                  WHERE s.assignment = a AND s.student.userId = :studentId)
            """)
    long countPendingAssignments(@Param("studentId") Long studentId,
                                 @Param("status") String status);

    /**
     * Bài tập chưa nộp, còn hạn, hạn gần nhất lên đầu. Truyền PageRequest.of(0, N) để lấy N bài.
     */
    @Query("""
            SELECT new com.japaneselms.account.student.UpcomingAssignmentDto(
                       a.assignmentId, a.title, c.courseId, c.courseName, a.dueAt)
            FROM Assignment a JOIN a.course c
            WHERE a.assignmentStatus = :status
              AND c.courseStatus = 'ACTIVE'
              AND a.dueAt IS NOT NULL
              AND a.dueAt >= :now
              AND c.courseId IN (
                  SELECT e.course.courseId FROM Enrollment e WHERE e.student.userId = :studentId)
              AND NOT EXISTS (
                  SELECT s.submissionId FROM Submission s
                  WHERE s.assignment = a AND s.student.userId = :studentId)
            ORDER BY a.dueAt ASC
            """)
    List<UpcomingAssignmentDto> findUpcomingAssignments(@Param("studentId") Long studentId,
                                                        @Param("status") String status,
                                                        @Param("now") LocalDateTime now,
                                                        Pageable pageable);

    @Query("""
            SELECT COUNT(s) FROM Submission s
            WHERE s.student.userId = :studentId AND s.grade IS NOT NULL
            """)
    long countGradedSubmissions(@Param("studentId") Long studentId);

    /**
     * null nếu chưa có bài nào được chấm.
     */
    @Query("""
            SELECT AVG(s.grade) FROM Submission s
            WHERE s.student.userId = :studentId AND s.grade IS NOT NULL
            """)
    Double averageGrade(@Param("studentId") Long studentId);
}
