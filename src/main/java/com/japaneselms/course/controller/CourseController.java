package com.japaneselms.course.controller;

import com.japaneselms.course.dto.CourseDetailDto;
import com.japaneselms.course.dto.CourseLevel;
import com.japaneselms.course.dto.CourseSummaryDto;
import com.japaneselms.course.service.CourseService;
import com.japaneselms.course.service.EnrollmentService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * S06 – Course List (UC06) và S07 – Course Details (UC07).
 * Cả hai là GET công khai (Guest được xem) nên Authentication có thể null.
 */
@Controller
@RequestMapping("/courses")
public class CourseController {

    private final CourseService courseService;
    private final EnrollmentService enrollmentService;

    public CourseController(CourseService courseService, EnrollmentService enrollmentService) {
        this.courseService = courseService;
        this.enrollmentService = enrollmentService;
    }

    /**
     * GET /courses?keyword=&level=&sort=createdAt,desc&page=0&size=9
     * Pageable được Spring Data Web Support tự bind từ page/size/sort.
     */
    @GetMapping
    public String listCourses(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String level,
            @PageableDefault(size = 9, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            Model model) {

        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        String normalizedLevel = CourseLevel.normalize(level);

        Page<CourseSummaryDto> courses =
                courseService.searchCourses(normalizedKeyword, normalizedLevel, pageable);

        // Giá trị sort đang áp dụng (sau khi Service lọc whitelist) để select ở view chọn đúng option
        String sortValue = courses.getSort().stream().findFirst()
                .map(order -> order.getProperty() + "," + order.getDirection().name().toLowerCase())
                .orElse("createdAt,desc");

        model.addAttribute("courses", courses);
        model.addAttribute("keyword", normalizedKeyword);
        model.addAttribute("level", normalizedLevel);
        model.addAttribute("sort", sortValue);
        model.addAttribute("levels", CourseLevel.values());
        model.addAttribute("currentPage", courses.getNumber());
        model.addAttribute("totalPages", courses.getTotalPages());
        model.addAttribute("totalElements", courses.getTotalElements());

        return "course/list";
    }

    /**
     * GET /courses/{id} – chi tiết khóa học ACTIVE + nút hành động theo vai trò.
     */
    @GetMapping("/{id}")
    public String viewCourse(@PathVariable Long id, Authentication authentication, Model model) {

        CourseDetailDto course = courseService.getActiveCourseDetail(id);

        // Chỉ Student mới cần biết "đã đăng ký chưa"; Guest/Instructor/Admin luôn là false
        boolean enrolled = false;
        if (AuthUtils.hasRole(authentication, "STUDENT")) {
            Long studentId = AuthUtils.requireUserId(authentication);
            enrolled = enrollmentService.isEnrolled(studentId, id);
        }

        model.addAttribute("course", course);
        model.addAttribute("enrolled", enrolled);

        return "course/detail";
    }
}
