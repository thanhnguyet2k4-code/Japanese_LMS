package com.japaneselms.course.controller;

import com.japaneselms.course.dto.CourseForm;
import com.japaneselms.course.service.CourseManagementService;
import com.japaneselms.entity.User;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class InstructorCourseController {

    private final CourseManagementService courseService;

    public InstructorCourseController(CourseManagementService courseService) {
        this.courseService = courseService;
    }

    @GetMapping("/instructor/courses/new")
    public String showCreateForm(Model model) {
        model.addAttribute("courseForm", new CourseForm());
        model.addAttribute("isEdit", false);
        model.addAttribute("currentTab", "courses");
        return "instructor/course-form";
    }

    @PostMapping("/instructor/courses")
    public String createCourse(
            @AuthenticationPrincipal User currentUser,
            @Valid @ModelAttribute("courseForm") CourseForm courseForm,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("isEdit", false);
            model.addAttribute("currentTab", "courses");
            return "instructor/course-form";
        }

        try {
            courseService.createCourse(currentUser, courseForm);
            redirectAttributes.addFlashAttribute("successMessage", "Đã tạo khóa học mới.");
            return "redirect:/instructor";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("isEdit", false);
            model.addAttribute("currentTab", "courses");
            model.addAttribute("errorMessage", ex.getMessage());
            return "instructor/course-form";
        }
    }

    @GetMapping("/instructor/courses/{courseId}/edit")
    public String showEditForm(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long courseId,
            Model model) {
        model.addAttribute("courseForm", courseService.getForm(currentUser, courseId));
        model.addAttribute("courseId", courseId);
        model.addAttribute("isEdit", true);
        model.addAttribute("currentTab", "courses");
        return "instructor/course-form";
    }

    @PostMapping("/instructor/courses/{courseId}/edit")
    public String updateCourse(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long courseId,
            @Valid @ModelAttribute("courseForm") CourseForm courseForm,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            courseService.getForm(currentUser, courseId);
            model.addAttribute("courseId", courseId);
            model.addAttribute("isEdit", true);
            model.addAttribute("currentTab", "courses");
            return "instructor/course-form";
        }

        try {
            courseService.updateCourse(currentUser, courseId, courseForm);
            redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật khóa học.");
            return "redirect:/instructor";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("courseId", courseId);
            model.addAttribute("isEdit", true);
            model.addAttribute("currentTab", "courses");
            model.addAttribute("errorMessage", ex.getMessage());
            return "instructor/course-form";
        }
    }

    @PostMapping("/instructor/courses/{courseId}/delete")
    public String deleteCourse(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long courseId,
            RedirectAttributes redirectAttributes) {
        try {
            courseService.deleteCourse(currentUser, courseId);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa khóa học.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/instructor";
    }

    @GetMapping("/files/course-images/{courseId}")
    public ResponseEntity<Resource> courseImage(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long courseId) {
        return courseService.getCourseImageResponse(currentUser, courseId);
    }
}
