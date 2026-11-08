package com.japaneselms.account.student;

import com.japaneselms.entity.User;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Student Dashboard – GET /student.
 * Quyền STUDENT đã được SecurityConfig chặn sẵn ("/student/**"); studentId lấy từ Authentication.
 */
@Controller
public class StudentDashboardController {

    private final StudentDashboardService dashboardService;

    public StudentDashboardController(StudentDashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/student")
    public String dashboard(Authentication authentication, Model model) {
        // studentId lấy từ phiên đăng nhập (principal là Entity User), KHÔNG nhận từ request (chống IDOR)
        if (!(authentication.getPrincipal() instanceof User user) || user.getUserId() == null) {
            throw new AccessDeniedException("Không xác định được người dùng đăng nhập.");
        }
        Long studentId = user.getUserId();
        model.addAttribute("dashboard", dashboardService.getDashboard(studentId));
        model.addAttribute("currentTab", "dashboard");   // đánh dấu mục đang chọn trên Sidebar
        return "student/dashboard";
    }
}
