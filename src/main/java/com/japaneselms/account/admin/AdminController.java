package com.japaneselms.account.admin;

import com.japaneselms.account.service.RegistrationService;
import com.japaneselms.entity.User;
import com.japaneselms.repository.UserRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Collections;
import java.util.List;

@Controller
public class AdminController {

    private final UserRepository userRepository;
    private final RegistrationService registrationService;

    public AdminController(UserRepository userRepository, RegistrationService registrationService) {
        this.userRepository = userRepository;
        this.registrationService = registrationService;
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
            // 1. Thống kê người dùng
            long totalUsers = userRepository.count();

            // 2. Danh sách & số lượng yêu cầu học viên chờ duyệt
            List<User> pendingStudents = registrationService.getPendingStudents();
            if (pendingStudents == null) {
                pendingStudents = Collections.emptyList();
            }
            long pendingRegistrationsCount = pendingStudents.size();

            // 3. Khóa học và đánh giá (Mặc định 0 khi chưa có dữ liệu để kích hoạt trạng thái Rỗng - Empty State)
            long activeCoursesCount = 0L;
            long pendingGradingCount = 0L;

            // 4. Phân bổ JLPT (Khởi tạo chuẩn 0% và 0 khóa học để tránh lỗi hiển thị kéo dài thanh)
            long n5Count = 0L;
            long n4Count = 0L;
            long n3Count = 0L;
            int n5Percent = 0;
            int n4Percent = 0;
            int n3Percent = 0;

            if (activeCoursesCount > 0) {
                n5Percent = (int) Math.round(((double) n5Count / activeCoursesCount) * 100);
                n4Percent = (int) Math.round(((double) n4Count / activeCoursesCount) * 100);
                n3Percent = (int) Math.round(((double) n3Count / activeCoursesCount) * 100);
            }

            // Đưa dữ liệu vào Model
            model.addAttribute("totalUsers", totalUsers);
            model.addAttribute("pendingRegistrationsCount", pendingRegistrationsCount);
            model.addAttribute("pendingStudents", pendingStudents);
            model.addAttribute("activeCoursesCount", activeCoursesCount);
            model.addAttribute("pendingGradingCount", pendingGradingCount);

            model.addAttribute("n5CourseCount", n5Count);
            model.addAttribute("n4CourseCount", n4Count);
            model.addAttribute("n3CourseCount", n3Count);
            model.addAttribute("n5Percentage", n5Percent);
            model.addAttribute("n4Percentage", n4Percent);
            model.addAttribute("n3Percentage", n3Percent);

            // Số lượng thông báo chưa đọc = số yêu cầu đăng ký chờ duyệt
            model.addAttribute("unreadNotificationsCount", pendingRegistrationsCount);

            // Đánh dấu tab đang kích hoạt trên Sidebar
            model.addAttribute("currentTab", "home");
            model.addAttribute("dashboardStatus", "SUCCESS");

        } catch (Exception e) {
            // Trạng thái Error State phục vụ nguyên tắc Dung sai & Khả năng phục hồi
            model.addAttribute("dashboardStatus", "ERROR");
            model.addAttribute("errorMessage", "Không thể đồng bộ dữ liệu tổng quan: " + e.getMessage());
            model.addAttribute("totalUsers", 0L);
            model.addAttribute("pendingRegistrationsCount", 0L);
            model.addAttribute("pendingStudents", Collections.emptyList());
            model.addAttribute("activeCoursesCount", 0L);
            model.addAttribute("pendingGradingCount", 0L);
            model.addAttribute("n5Percentage", 0);
            model.addAttribute("n4Percentage", 0);
            model.addAttribute("n3Percentage", 0);
            model.addAttribute("currentTab", "home");
        }

        return "admin/home";
    }
}
