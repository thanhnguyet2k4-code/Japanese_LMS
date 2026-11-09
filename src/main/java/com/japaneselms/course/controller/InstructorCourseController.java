
package com.japaneselms.course.controller;

import com.japaneselms.course.dto.CourseForm;
import com.japaneselms.course.dto.CourseLevel;
import com.japaneselms.course.dto.CourseSummaryDto;
import com.japaneselms.course.exception.DuplicateCourseNameException;
import com.japaneselms.course.service.CourseManagementService;
import com.japaneselms.course.service.InstructorCourseService;
import com.japaneselms.entity.User;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.multipart.MultipartFile;

/**
 * Quan ly khoa hoc cua Instructor.
 * MF-02: Danh sach, tao, sua khoa hoc (chon trang thai ACTIVE/INACTIVE).
 * MF-03: Xoa khoa hoc.
 * MF-03: Upload va hien thi anh bia khoa hoc.
 */
@Controller
@RequestMapping("/instructor/courses")
public class InstructorCourseController {

    private static final String VIEW = "instructor/course-form";

    private final InstructorCourseService instructorCourseService;
    private final CourseManagementService courseManagementService;

    public InstructorCourseController(
            InstructorCourseService instructorCourseService,
            CourseManagementService courseManagementService) {
        this.instructorCourseService = instructorCourseService;
        this.courseManagementService = courseManagementService;
    }

    // ==================== DANH SACH KHOA HOC ====================

    @GetMapping
    public String list(
            @PageableDefault(
                    size = 10,
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            ) Pageable pageable,
            Authentication authentication,
            Model model) {

        Long instructorId = AuthUtils.requireUserId(authentication);

        Page<CourseSummaryDto> courses =
                instructorCourseService.getMyCourses(instructorId, pageable);

        model.addAttribute("mode", "list");
        model.addAttribute("courses", courses);
        model.addAttribute("currentPage", courses.getNumber());
        model.addAttribute("totalPages", courses.getTotalPages());
        model.addAttribute("totalElements", courses.getTotalElements());
        model.addAttribute("currentTab", "instructor-courses");

        return VIEW;
    }

    // ==================== TAO KHOA HOC ====================

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        prepareForm(model, new CourseForm(), null);
        return VIEW;
    }

    @PostMapping("/new")
    public String create(
            @AuthenticationPrincipal User currentUser,
            @Valid @ModelAttribute("courseForm") CourseForm courseForm,
            BindingResult bindingResult,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            prepareForm(model, null, null);
            return VIEW;
        }

        Long instructorId = AuthUtils.requireUserId(authentication);
        Long courseId;

        try {
            courseId = instructorCourseService.createCourse(
                    courseForm, instructorId);

        } catch (DuplicateCourseNameException ex) {
            bindingResult.rejectValue(
                    "courseName", "duplicate", ex.getMessage());
            prepareForm(model, null, null);
            return VIEW;
        }

        // Upload anh sau khi tao khoa hoc thanh cong.
        MultipartFile coverImage = courseForm.getCoverImage();

        if (coverImage != null && !coverImage.isEmpty()) {
            try {
                courseManagementService.updateCourseImage(
                        currentUser, courseId, coverImage);

            } catch (IllegalArgumentException ex) {
                redirectAttributes.addFlashAttribute(
                        "errorMessage",
                        "Khóa học đã được tạo nhưng không thể cập nhật ảnh bìa: "
                                + ex.getMessage());

                return "redirect:/instructor/courses";
            }
        }

        redirectAttributes.addFlashAttribute(
                "successMessage", "Đã tạo khóa học mới.");

        return "redirect:/instructor/courses";
    }

    // ==================== SUA KHOA HOC ====================

    @GetMapping("/{id}/edit")
    public String showEditForm(
            @PathVariable Long id,
            Authentication authentication,
            Model model) {

        Long instructorId = AuthUtils.requireUserId(authentication);

        CourseForm form =
                instructorCourseService.getCourseForm(id, instructorId);

        prepareForm(model, form, id);
        return VIEW;
    }

    @PostMapping("/{id}/edit")
    public String update(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser,
            @Valid @ModelAttribute("courseForm") CourseForm courseForm,
            BindingResult bindingResult,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes) {

        Long instructorId = AuthUtils.requireUserId(authentication);

        if (bindingResult.hasErrors()) {
            prepareForm(model, null, id);
            return VIEW;
        }

        try {
            // Cap nhat thong tin khoa hoc.
            instructorCourseService.updateCourse(
                    id, courseForm, instructorId);

            // Cap nhat anh bia neu Instructor chon anh moi.
            MultipartFile coverImage = courseForm.getCoverImage();

            if (coverImage != null && !coverImage.isEmpty()) {
                courseManagementService.updateCourseImage(
                        currentUser, id, coverImage);
            }

        } catch (DuplicateCourseNameException ex) {
            bindingResult.rejectValue(
                    "courseName", "duplicate", ex.getMessage());

            prepareForm(model, null, id);
            return VIEW;

        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Thông tin khóa học đã được cập nhật nhưng ảnh bìa không hợp lệ: "
                            + ex.getMessage());

            return "redirect:/instructor/courses";
        }

        redirectAttributes.addFlashAttribute(
                "successMessage", "Đã cập nhật khóa học.");

        return "redirect:/instructor/courses";
    }

    // ==================== XOA KHOA HOC (chuc nang cua Thang) ====================

    /**
     * Xoa han khoa hoc cung bai hoc, tai lieu, tien do va danh sach hoc vien.
     * Khoa hoc da co bai tap thi bi tu choi (Instructor co the doi sang INACTIVE o man hinh sua).
     */
    @PostMapping("/{id}/delete")
    public String delete(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long id,
            @RequestParam(value = "from", required = false) String from,
            RedirectAttributes redirectAttributes) {

        // Chi chap nhan gia tri co dinh de tranh open redirect.
        String target = "lessons".equals(from)
                ? "redirect:/instructor/lesson-courses"
                : "redirect:/instructor/courses";

        try {
            courseManagementService.deleteCourse(currentUser, id);

            redirectAttributes.addFlashAttribute(
                    "successMessage", "Đã xóa khóa học.");

        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage", ex.getMessage());

        } catch (DataIntegrityViolationException ex) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Không thể xóa khóa học vì còn dữ liệu liên quan. "
                            + "Hãy chuyển khóa học sang trạng thái INACTIVE.");
        }

        return target;
    }


    // ==================== HAM DUNG CHUNG ====================

    private void prepareForm(
            Model model,
            CourseForm form,
            Long courseId) {

        if (form != null) {
            model.addAttribute("courseForm", form);
        }

        model.addAttribute("mode", "form");
        model.addAttribute("isEdit", courseId != null);
        model.addAttribute("courseId", courseId);
        model.addAttribute("levels", CourseLevel.values());
        model.addAttribute("currentTab", "instructor-courses");
    }
}
