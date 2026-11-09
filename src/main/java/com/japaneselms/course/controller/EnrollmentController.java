package com.japaneselms.course.controller;

import com.japaneselms.course.dto.MyCourseDto;
import com.japaneselms.course.exception.EnrollmentException;
import com.japaneselms.course.exception.EnrollmentException.Reason;
import com.japaneselms.course.service.EnrollmentService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * UC09 – POST /courses/{id}/enroll   và   UC10 – GET /my-courses (S08).
 * Quyền STUDENT được chặn ở SecurityConfig; studentId lấy từ Authentication.
 */
@Controller
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    public EnrollmentController(EnrollmentService enrollmentService) {
        this.enrollmentService = enrollmentService;
    }

    // ==================== UC09: ĐĂNG KÝ KHÓA HỌC ====================

    @PostMapping("/courses/{id}/enroll")
    public String enroll(@PathVariable Long id,
                         Authentication authentication,
                         RedirectAttributes redirectAttributes) {

        Long studentId = AuthUtils.requireUserId(authentication);

        try {
            enrollmentService.enroll(studentId, id);

            redirectAttributes.addFlashAttribute("successMessage",
                    "Đăng ký khóa học thành công! Chúc bạn học tốt.");
            return "redirect:/my-courses";

        } catch (EnrollmentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());

            // E4: khóa học đã ngừng hoạt động -> về danh sách; các lỗi còn lại -> về lại S07
            return ex.getReason() == Reason.COURSE_INACTIVE
                    ? "redirect:/courses"
                    : "redirect:/courses/" + id;

        } catch (DataIntegrityViolationException ex) {
            // E5: hai yêu cầu đến cùng lúc, vi phạm UNIQUE(student_id, course_id) -> coi như đã đăng ký
            redirectAttributes.addFlashAttribute("errorMessage", "Bạn đã đăng ký khóa học này.");
            return "redirect:/courses/" + id;
        }
    }

    // ==================== UC10: KHÓA HỌC CỦA TÔI (S08) ====================

    @GetMapping("/my-courses")
    public String myCourses(
            @PageableDefault(size = 9, sort = "enrolledAt", direction = Sort.Direction.DESC) Pageable pageable,
            Authentication authentication,
            Model model) {

        // KHÔNG nhận studentId từ request -> không thể xem khóa học của người khác (IDOR)
        Long studentId = AuthUtils.requireUserId(authentication);

        Page<MyCourseDto> enrollments = enrollmentService.getMyEnrollments(studentId, pageable);

        model.addAttribute("enrollments", enrollments);
        model.addAttribute("currentPage", enrollments.getNumber());
        model.addAttribute("totalPages", enrollments.getTotalPages());
        model.addAttribute("totalElements", enrollments.getTotalElements());

        return "student/my-courses";
    }
}
