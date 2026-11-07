package com.japaneselms.account.instructor;

import com.japaneselms.entity.User;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Instructor Dashboard – GET /instructor.
 * Quyền INSTRUCTOR đã được SecurityConfig chặn sẵn ("/instructor/**"); instructorId lấy từ Authentication.
 */
@Controller
public class InstructorDashboardController {

    private final InstructorDashboardService dashboardService;

    public InstructorDashboardController(InstructorDashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/instructor")
    public String dashboard(Authentication authentication, Model model) {
        // instructorId lấy từ phiên đăng nhập (principal là Entity User), KHÔNG nhận từ request (chống IDOR)
        if (!(authentication.getPrincipal() instanceof User user) || user.getUserId() == null) {
            throw new AccessDeniedException("Không xác định được người dùng đăng nhập.");
        }
        Long instructorId = user.getUserId();
        model.addAttribute("dashboard", dashboardService.getDashboard(instructorId));
        model.addAttribute("currentTab", "dashboard");   // đánh dấu mục đang chọn trên Sidebar
        return "instructor/dashboard";
    }
}
