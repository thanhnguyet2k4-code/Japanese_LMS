package com.japaneselms.course.controller;

import com.japaneselms.course.dto.CourseDetailDto;
import com.japaneselms.course.dto.CourseSummaryDto;
import com.japaneselms.course.dto.MyCourseDto;
import com.japaneselms.course.service.CourseService;
import com.japaneselms.course.service.EnrollmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;
    private final EnrollmentService enrollmentService;

    /**
     * S06 - Course List (UC06): Duyệt, tìm kiếm, lọc theo N5/N4/N3, phân trang và sắp xếp
     */
    @GetMapping("/courses")
    public String listCourses(
            @RequestParam(required = false, defaultValue = "") String keyword,
            @RequestParam(required = false, defaultValue = "") String level,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "9") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            Model model
    ) {
        if (level != null && !level.isBlank() && !CourseService.ALLOWED_LEVELS.contains(level.trim().toUpperCase())) {
            model.addAttribute("warningMessage", "Cấp độ lọc không hợp lệ, hệ thống đã hiển thị tất cả cấp độ.");
            level = "";
        }

        Page<CourseSummaryDto> coursePage = courseService.searchActiveCourses(keyword, level, page, size, sortBy, sortDir);

        model.addAttribute("courses", coursePage);
        model.addAttribute("keyword", keyword != null ? keyword.trim() : "");
        model.addAttribute("level", level != null ? level.trim().toUpperCase() : "");
        model.addAttribute("levels", CourseService.ALLOWED_LEVELS);
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("sortDir", sortDir);
        model.addAttribute("currentPage", coursePage.getNumber());
        model.addAttribute("totalPages", coursePage.getTotalPages());
        model.addAttribute("totalElements", coursePage.getTotalElements());

        return "course/list";
    }

    /**
     * S07 - Course Details (UC07): Xem chi tiết khóa học
     */
    @GetMapping("/courses/{id}")
    public String viewCourseDetails(
            @PathVariable("id") Long id,
            Authentication auth,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        try {
            String email = (auth != null && auth.isAuthenticated()) ? auth.getName() : null;
            CourseDetailDto courseDetail = courseService.getActiveCourseDetail(id, email);
            model.addAttribute("course", courseDetail);
            return "course/detail";
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/courses";
        }
    }

    /**
     * UC09 - Enroll in Course: Đăng ký tham gia khóa học từ màn hình S07
     */
    @PostMapping("/courses/{id}/enroll")
    public String enrollCourse(
            @PathVariable("id") Long id,
            Authentication auth,
            RedirectAttributes redirectAttributes
    ) {
        if (auth == null || !auth.isAuthenticated()) {
            return "redirect:/login";
        }
        try {
            enrollmentService.enrollCourse(id, auth.getName());
            redirectAttributes.addFlashAttribute("successMessage", "Đăng ký khóa học thành công!");
            return "redirect:/my-courses";
        } catch (IllegalStateException | IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/courses/" + id;
        }
    }

    /**
     * S08 - My Courses (UC10): Xem danh sách khóa học đã đăng ký của Student
     */
    @GetMapping("/my-courses")
    public String viewMyCourses(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "9") int size,
            Authentication auth,
            Model model
    ) {
        Page<MyCourseDto> enrollmentPage = enrollmentService.getMyCourses(auth.getName(), page, size);
        model.addAttribute("enrollments", enrollmentPage);
        model.addAttribute("currentPage", enrollmentPage.getNumber());
        model.addAttribute("totalPages", enrollmentPage.getTotalPages());
        model.addAttribute("totalElements", enrollmentPage.getTotalElements());
        return "course/my-courses";
    }
}