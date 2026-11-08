package com.japaneselms.account.student;

import com.japaneselms.course.dto.MyCourseDto;
import com.japaneselms.entity.User;
import com.japaneselms.repository.EnrollmentRepository;
import com.japaneselms.repository.StudentDashboardRepository;
import com.japaneselms.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Student Dashboard. studentId luôn do Controller lấy từ SecurityContext (chống IDOR).
 */
@Service
public class StudentDashboardService {

    /**
     * Trạng thái của bài tập được coi là "đang mở" cho học viên.
     * !!! Đối chiếu với giá trị thật của cột assignments.assignment_status trong DB
     * (Member phụ trách Assignment quy định) và sửa tại ĐÂY nếu khác.
     */
    static final String ASSIGNMENT_OPEN_STATUS = "ACTIVE";

    private static final int RECENT_COURSES = 3;
    private static final int UPCOMING_ASSIGNMENTS = 5;

    private final StudentDashboardRepository dashboardRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;

    public StudentDashboardService(StudentDashboardRepository dashboardRepository,
                                   EnrollmentRepository enrollmentRepository,
                                   UserRepository userRepository) {
        this.dashboardRepository = dashboardRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public StudentDashboardDto getDashboard(Long studentId) {
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new AccessDeniedException("Không tìm thấy tài khoản đăng nhập."));

        List<MyCourseDto> recentCourses = enrollmentRepository
                .findByStudent_UserId(studentId,
                        PageRequest.of(0, RECENT_COURSES, Sort.by(Sort.Direction.DESC, "enrolledAt")))
                .map(MyCourseDto::from)
                .getContent();

        return new StudentDashboardDto(
                student.getFullName(),
                dashboardRepository.countEnrollments(studentId),
                dashboardRepository.countActiveCourses(studentId),
                dashboardRepository.countAvailableLessons(studentId),
                dashboardRepository.countPendingAssignments(studentId, ASSIGNMENT_OPEN_STATUS),
                dashboardRepository.countGradedSubmissions(studentId),
                dashboardRepository.averageGrade(studentId),
                recentCourses,
                dashboardRepository.findUpcomingAssignments(
                        studentId, ASSIGNMENT_OPEN_STATUS, LocalDateTime.now(),
                        PageRequest.of(0, UPCOMING_ASSIGNMENTS)));
    }
}
