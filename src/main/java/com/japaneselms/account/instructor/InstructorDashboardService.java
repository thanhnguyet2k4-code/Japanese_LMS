package com.japaneselms.account.instructor;

import com.japaneselms.entity.User;
import com.japaneselms.repository.InstructorDashboardRepository;
import com.japaneselms.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Instructor Dashboard. instructorId luôn do Controller lấy từ SecurityContext (chống IDOR).
 */
@Service
public class InstructorDashboardService {

    private static final int RECENT_COURSES = 5;
    private static final int RECENT_ENROLLMENTS = 5;

    private final InstructorDashboardRepository dashboardRepository;
    private final UserRepository userRepository;

    public InstructorDashboardService(InstructorDashboardRepository dashboardRepository,
                                      UserRepository userRepository) {
        this.dashboardRepository = dashboardRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public InstructorDashboardDto getDashboard(Long instructorId) {
        User instructor = userRepository.findById(instructorId)
                .orElseThrow(() -> new AccessDeniedException("Không tìm thấy tài khoản đăng nhập."));

        return new InstructorDashboardDto(
                instructor.getFullName(),
                dashboardRepository.countCourses(instructorId),
                dashboardRepository.countActiveCourses(instructorId),
                dashboardRepository.countDistinctStudents(instructorId),
                dashboardRepository.countUngradedSubmissions(instructorId),
                dashboardRepository.findRecentCoursesWithStudentCount(
                        instructorId, PageRequest.of(0, RECENT_COURSES)),
                dashboardRepository.findRecentEnrollments(
                        instructorId, PageRequest.of(0, RECENT_ENROLLMENTS)));
    }
}
