package com.japaneselms.course.controller;

import com.japaneselms.course.dto.CourseFormDto;
import com.japaneselms.course.dto.CourseSummaryDto;
import com.japaneselms.course.service.CourseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/instructor/courses")
@RequiredArgsConstructor
public class InstructorCourseController {

    private final CourseService courseService;

    /**
     * S09: Hiển thị danh sách khóa học của Instructor kèm biểu mẫu Tạo mới
     */
    @GetMapping
    public String manageCourses(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "9") int size,
            Authentication auth,
            Model model
    ) {
        if (!model.containsAttribute("courseForm")) {
            model.addAttribute("courseForm", new CourseFormDto());
        }
        model.addAttribute("editMode", false);
        populateInstructorCourseList(auth.getName(), page, size, model);
        return "course/instructor-courses";
    }

    /**
     * S09: Mở chế độ tạo mới khóa học
     */
    @GetMapping("/new")
    public String showCreateForm(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "9") int size,
            Authentication auth,
            Model model
    ) {
        model.addAttribute("courseForm", new CourseFormDto());
        model.addAttribute("editMode", false);
        populateInstructorCourseList(auth.getName(), page, size, model);
        return "course/instructor-courses";
    }

    /**
     * S09: Xử lý POST tạo mới khóa học
     */
    @PostMapping
    public String createCourse(
            @Valid @ModelAttribute("courseForm") CourseFormDto courseForm,
            BindingResult bindingResult,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "9") int size,
            Authentication auth,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (!bindingResult.hasFieldErrors("courseName")
                && courseService.isDuplicateCourseName(auth.getName(), courseForm.getCourseName(), null)) {
            bindingResult.rejectValue("courseName", "duplicate", "Tên khóa học này đã tồn tại trong danh sách của bạn");
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("editMode", false);
            populateInstructorCourseList(auth.getName(), page, size, model);
            return "course/instructor-courses";
        }

        courseService.createCourse(courseForm, auth.getName());
        redirectAttributes.addFlashAttribute("successMessage", "Tạo khóa học mới thành công!");
        return "redirect:/instructor/courses";
    }

    /**
     * S09: Nạp dữ liệu khóa học lên Form để chỉnh sửa
     */
    @GetMapping("/{id}/edit")
    public String showEditForm(
            @PathVariable("id") Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "9") int size,
            Authentication auth,
            Model model
    ) {
        CourseFormDto formDto = courseService.getCourseFormForEdit(id, auth.getName());
        model.addAttribute("courseForm", formDto);
        model.addAttribute("editMode", true);
        populateInstructorCourseList(auth.getName(), page, size, model);
        return "course/instructor-courses";
    }

    /**
     * S09: Xử lý POST cập nhật khóa học
     */
    @PostMapping("/{id}/edit")
    public String updateCourse(
            @PathVariable("id") Long id,
            @Valid @ModelAttribute("courseForm") CourseFormDto courseForm,
            BindingResult bindingResult,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "9") int size,
            Authentication auth,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        courseForm.setCourseId(id);

        if (!bindingResult.hasFieldErrors("courseName")
                && courseService.isDuplicateCourseName(auth.getName(), courseForm.getCourseName(), id)) {
            bindingResult.rejectValue("courseName", "duplicate", "Tên khóa học này đã tồn tại trong danh sách của bạn");
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("editMode", true);
            populateInstructorCourseList(auth.getName(), page, size, model);
            return "course/instructor-courses";
        }

        courseService.updateCourse(id, courseForm, auth.getName());
        redirectAttributes.addFlashAttribute("successMessage", "Cập nhật khóa học thành công!");
        return "redirect:/instructor/courses";
    }

    /**
     * S09: Ngừng hoạt động (Soft Delete) hoặc xóa khóa học nếu chưa có dữ liệu liên quan
     */
    @PostMapping("/{id}/deactivate")
    public String deactivateCourse(
            @PathVariable("id") Long id,
            Authentication auth,
            RedirectAttributes redirectAttributes
    ) {
        String resultMessage = courseService.deactivateOrDeleteCourse(id, auth.getName());
        redirectAttributes.addFlashAttribute("successMessage", resultMessage);
        return "redirect:/instructor/courses";
    }

    private void populateInstructorCourseList(String email, int page, int size, Model model) {
        Page<CourseSummaryDto> coursePage = courseService.getInstructorCourses(email, page, size);
        model.addAttribute("courses", coursePage);
        model.addAttribute("levels", CourseService.ALLOWED_LEVELS);
        model.addAttribute("currentPage", coursePage.getNumber());
        model.addAttribute("totalPages", coursePage.getTotalPages());
    }
}