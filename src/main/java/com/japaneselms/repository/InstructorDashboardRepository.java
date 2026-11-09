package com.japaneselms.repository;

import com.japaneselms.account.instructor.InstructorCourseRowDto;
import com.japaneselms.account.instructor.RecentEnrollmentDto;
import com.japaneselms.entity.Course;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Các truy vấn thống kê cho Instructor Dashboard.
 * File MỚI, tách riêng để không sửa các Repository của Leader / Member khác.
 */
public interface InstructorDashboardRepository extends Repository<Course, Long> {

    @Query("SELECT COUNT(c) FROM Course c WHERE c.instructor.userId = :instructorId")
    long countCourses(@Param("instructorId") Long instructorId);

    @Query("""
            SELECT COUNT(c) FROM Course c
            WHERE c.instructor.userId = :instructorId AND c.courseStatus = 'ACTIVE'
            """)
    long countActiveCourses(@Param("instructorId") Long instructorId);

    /**
     * Số học viên (không trùng) đã đăng ký ít nhất một khóa học của instructor.
     */
    @Query("""
            SELECT COUNT(DISTINCT e.student.userId) FROM Enrollment e
            WHERE e.course.instructor.userId = :instructorId
            """)
    long countDistinctStudents(@Param("instructorId") Long instructorId);

    /**
     * Bài nộp chưa được chấm điểm của các bài tập thuộc khóa học của instructor.
     */
    @Query("""
            SELECT COUNT(s) FROM Submission s
            WHERE s.grade IS NULL
              AND s.assignment.course.instructor.userId = :instructorId
            """)
    long countUngradedSubmissions(@Param("instructorId") Long instructorId);

    /**
     * Khóa học mới nhất của instructor kèm số học viên. Truyền PageRequest.of(0, N).
     */
    @Query("""
            SELECT new com.japaneselms.account.instructor.InstructorCourseRowDto(
                       c.courseId, c.courseName, c.jlptLevel, c.courseStatus, COUNT(e))
            FROM Course c LEFT JOIN Enrollment e ON e.course = c
            WHERE c.instructor.userId = :instructorId
            GROUP BY c.courseId, c.courseName, c.jlptLevel, c.courseStatus, c.createdAt
            ORDER BY c.createdAt DESC
            """)
    List<InstructorCourseRowDto> findRecentCoursesWithStudentCount(
            @Param("instructorId") Long instructorId, Pageable pageable);

    /**
     * Lượt đăng ký mới nhất vào các khóa học của instructor.
     */
    @Query("""
            SELECT new com.japaneselms.account.instructor.RecentEnrollmentDto(
                       s.fullName, s.email, c.courseName, e.enrolledAt)
            FROM Enrollment e JOIN e.student s JOIN e.course c
            WHERE c.instructor.userId = :instructorId
            ORDER BY e.enrolledAt DESC
            """)
    List<RecentEnrollmentDto> findRecentEnrollments(
            @Param("instructorId") Long instructorId, Pageable pageable);
}
