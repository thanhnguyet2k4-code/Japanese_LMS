package com.japaneselms.account.admin;

import com.japaneselms.account.service.RegistrationService;
import com.japaneselms.entity.Course;
import com.japaneselms.entity.Submission;
import com.japaneselms.entity.User;
import com.japaneselms.repository.CourseRepository;
import com.japaneselms.repository.SubmissionRepository;
import com.japaneselms.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
public class AdminController {

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

    private final UserRepository userRepository;
    private final RegistrationService registrationService;
    private final CourseRepository courseRepository;
    private final SubmissionRepository submissionRepository;

    public AdminController(
            UserRepository userRepository,
            RegistrationService registrationService,
            CourseRepository courseRepository,
            SubmissionRepository submissionRepository
    ) {
        this.userRepository = userRepository;
        this.registrationService = registrationService;
        this.courseRepository = courseRepository;
        this.submissionRepository = submissionRepository;
    }

    /**
     * Hiển thị trang Admin Dashboard (Tổng quan hệ thống)
     * Hỗ trợ route /admin và /admin/home
     */
    @GetMapping({"/admin", "/admin/home"})
    public String adminHome(
            @RequestParam(value = "refresh", required = false) Boolean refresh,
            Model model
    ) {
        try {
            // 1. Thống kê người dùng (MF-01)
            long totalUsers = userRepository.count();

            // 2. Danh sách & số lượng yêu cầu học viên chờ duyệt (MF-01)
            List<User> pendingStudents = registrationService.getPendingStudents();
            if (pendingStudents == null) {
                pendingStudents = Collections.emptyList();
            }
            long pendingRegistrationsCount = pendingStudents.size();

            // 3. Khóa học hoạt động & Phân bổ JLPT (MF-02 - Course Management)
            long activeCoursesCount = 0L;
            long n5Count = 0L;
            long n4Count = 0L;
            long n3Count = 0L;
            int n5Percent = 0;
            int n4Percent = 0;
            int n3Percent = 0;
            Map<String, Long> jlptDistribution = new LinkedHashMap<>();

            try {
                List<Course> allCourses = courseRepository.findAll();
                if (allCourses != null) {
                    activeCoursesCount = allCourses.stream()
                            .filter(c -> "ACTIVE".equalsIgnoreCase(c.getCourseStatus()))
                            .count();

                    n5Count = allCourses.stream()
                            .filter(c -> "ACTIVE".equalsIgnoreCase(c.getCourseStatus())
                                    && "N5".equalsIgnoreCase(c.getJlptLevel()))
                            .count();

                    n4Count = allCourses.stream()
                            .filter(c -> "ACTIVE".equalsIgnoreCase(c.getCourseStatus())
                                    && "N4".equalsIgnoreCase(c.getJlptLevel()))
                            .count();

                    n3Count = allCourses.stream()
                            .filter(c -> "ACTIVE".equalsIgnoreCase(c.getCourseStatus())
                                    && "N3".equalsIgnoreCase(c.getJlptLevel()))
                            .count();

                    if (activeCoursesCount > 0) {
                        n5Percent = (int) Math.round(((double) n5Count / activeCoursesCount) * 100);
                        n4Percent = (int) Math.round(((double) n4Count / activeCoursesCount) * 100);
                        n3Percent = (int) Math.round(((double) n3Count / activeCoursesCount) * 100);
                    }
                }
            } catch (Exception e) {
                log.warn("Lỗi khi đọc dữ liệu khóa học từ CourseRepository: {}", e.getMessage());
                activeCoursesCount = 0L;
                n5Count = 0L;
                n4Count = 0L;
                n3Count = 0L;
            }

            jlptDistribution.put("N5", n5Count);
            jlptDistribution.put("N4", n4Count);
            jlptDistribution.put("N3", n3Count);

            // 4. Bài nộp chờ chấm (MF-05 - Grading Management)
            long pendingGradingCount = 0L;
            try {
                List<Submission> allSubmissions = submissionRepository.findAll();
                if (allSubmissions != null) {
                    pendingGradingCount = allSubmissions.stream()
                            .filter(s -> s.getGrade() == null)
                            .count();
                }
            } catch (Exception e) {
                log.warn("Lỗi khi đọc dữ liệu bài nộp từ SubmissionRepository: {}", e.getMessage());
                pendingGradingCount = 0L;
            }

            // Đưa dữ liệu vào Model
            model.addAttribute("totalUsers", totalUsers);
            model.addAttribute("totalUsersCount", totalUsers);
            model.addAttribute("pendingRegistrationsCount", pendingRegistrationsCount);
            model.addAttribute("pendingStudents", pendingStudents);
            model.addAttribute("activeCoursesCount", activeCoursesCount);
            model.addAttribute("pendingGradingCount", pendingGradingCount);

            // Thống kê phân bổ JLPT
            model.addAttribute("jlptDistribution", jlptDistribution);
            model.addAttribute("n5CourseCount", n5Count);
            model.addAttribute("n4CourseCount", n4Count);
            model.addAttribute("n3CourseCount", n3Count);
            model.addAttribute("n5Percentage", n5Percent);
            model.addAttribute("n4Percentage", n4Percent);
            model.addAttribute("n3Percentage", n3Percent);

            // Số lượng thông báo chưa đọc = số yêu cầu đăng ký chờ duyệt
            model.addAttribute("unreadNotificationsCount", pendingRegistrationsCount);

            // Đánh dấu tab đang kích hoạt trên Sidebar
            model.addAttribute("currentTab", "dashboard");
            model.addAttribute("dashboardStatus", "SUCCESS");

        } catch (Exception e) {
            log.error("Lỗi khi tải trang quản trị Admin: {}", e.getMessage(), e);
            model.addAttribute("dashboardStatus", "ERROR");
            model.addAttribute("errorMessage", "Không thể đồng bộ dữ liệu tổng quan: " + e.getMessage());
            model.addAttribute("totalUsers", 0L);
            model.addAttribute("totalUsersCount", 0L);
            model.addAttribute("pendingRegistrationsCount", 0L);
            model.addAttribute("pendingStudents", Collections.emptyList());
            model.addAttribute("activeCoursesCount", 0L);
            model.addAttribute("pendingGradingCount", 0L);
            Map<String, Long> emptyDistribution = new LinkedHashMap<>();
            emptyDistribution.put("N5", 0L);
            emptyDistribution.put("N4", 0L);
            emptyDistribution.put("N3", 0L);
            model.addAttribute("jlptDistribution", emptyDistribution);
            model.addAttribute("n5CourseCount", 0L);
            model.addAttribute("n4CourseCount", 0L);
            model.addAttribute("n3CourseCount", 0L);
            model.addAttribute("n5Percentage", 0);
            model.addAttribute("n4Percentage", 0);
            model.addAttribute("n3Percentage", 0);
            model.addAttribute("unreadNotificationsCount", 0L);
            model.addAttribute("currentTab", "dashboard");
        }

        return "admin/home";
    }
}
