package com.japaneselms.learning.controller;

import com.japaneselms.common.ListPaging;
import com.japaneselms.entity.Course;
import com.japaneselms.entity.CourseSection;
import com.japaneselms.entity.Lesson;
import com.japaneselms.entity.User;
import com.japaneselms.learning.dto.LessonForm;
import com.japaneselms.learning.service.LearningMaterialService;
import com.japaneselms.learning.service.LessonService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class InstructorLessonController {

    private final LessonService lessonService;
    private final LearningMaterialService materialService;

    public InstructorLessonController(
            LessonService lessonService,
            LearningMaterialService materialService) {
        this.lessonService = lessonService;
        this.materialService = materialService;
    }

    // Trang danh sach khoa hoc cua giang vien
    @GetMapping("/instructor/lesson-courses")
    public String instructorHome(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String page,
            Model model) {
        ListPaging.populateInstructorCourses(model, lessonService.listInstructorCourses(currentUser),
                keyword, level, status, sort, page);
        model.addAttribute("currentTab", "lessons");
        return "instructor/courses";
    }

    // S10: Danh sach bai hoc trong khoa hoc
    @GetMapping("/instructor/courses/{courseId}/lessons")
    public String listLessons(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long courseId,
            Model model) {
        Course course = lessonService.getManageableCourse(currentUser, courseId);
        model.addAttribute("course", course);
        model.addAttribute("groups", lessonService.groupLessonsForInstructor(currentUser, courseId));
        model.addAttribute("currentTab", "lessons");
        return "instructor/lessons/list";
    }

    // S11: Hien thi form tao bai hoc moi
    @GetMapping("/instructor/courses/{courseId}/lessons/new")
    public String showCreateForm(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long courseId,
            @RequestParam(required = false) Long sectionId,
            Model model) {
        Course course = lessonService.getManageableCourse(currentUser, courseId);
        LessonForm lessonForm = new LessonForm();
        lessonForm.setSectionId(sectionId);

        model.addAttribute("course", course);
        model.addAttribute("sections", lessonService.listSections(currentUser, courseId));
        model.addAttribute("lessonForm", lessonForm);
        model.addAttribute("isEdit", false);
        model.addAttribute("currentTab", "lessons");
        return "instructor/lessons/form";
    }

    // S11: Xu ly tao bai hoc moi
    @PostMapping("/instructor/courses/{courseId}/lessons")
    public String createLesson(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long courseId,
            @Valid @ModelAttribute("lessonForm") LessonForm lessonForm,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        Course course = lessonService.getManageableCourse(currentUser, courseId);

        if (bindingResult.hasErrors()) {
            model.addAttribute("course", course);
            model.addAttribute("sections", lessonService.listSections(currentUser, courseId));
            model.addAttribute("isEdit", false);
            model.addAttribute("currentTab", "lessons");
            return "instructor/lessons/form";
        }

        try {
            lessonService.createLesson(currentUser, courseId, lessonForm);
            redirectAttributes.addFlashAttribute("successMessage", "Đã tạo bài học mới.");
            return "redirect:/instructor/courses/" + courseId + "/lessons";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("course", course);
            model.addAttribute("sections", lessonService.listSections(currentUser, courseId));
            model.addAttribute("isEdit", false);
            model.addAttribute("currentTab", "lessons");
            model.addAttribute("errorMessage", ex.getMessage());
            return "instructor/lessons/form";
        }
    }

    // S11: Hien thi form chinh sua bai hoc
    @GetMapping("/instructor/courses/{courseId}/lessons/{lessonId}/edit")
    public String showEditForm(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long courseId,
            @PathVariable Long lessonId,
            Model model) {
        Course course = lessonService.getManageableCourse(currentUser, courseId);
        LessonForm form = lessonService.getLessonForm(currentUser, lessonId);

        model.addAttribute("course", course);
        model.addAttribute("sections", lessonService.listSections(currentUser, courseId));
        model.addAttribute("lessonForm", form);
        model.addAttribute("lessonId", lessonId);
        model.addAttribute("isEdit", true);
        model.addAttribute("currentTab", "lessons");
        return "instructor/lessons/form";
    }

    // S11: Xu ly cap nhat bai hoc
    @PostMapping("/instructor/courses/{courseId}/lessons/{lessonId}/edit")
    public String updateLesson(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long courseId,
            @PathVariable Long lessonId,
            @Valid @ModelAttribute("lessonForm") LessonForm lessonForm,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        Course course = lessonService.getManageableCourse(currentUser, courseId);

        if (bindingResult.hasErrors()) {
            model.addAttribute("course", course);
            model.addAttribute("sections", lessonService.listSections(currentUser, courseId));
            model.addAttribute("lessonId", lessonId);
            model.addAttribute("isEdit", true);
            model.addAttribute("currentTab", "lessons");
            return "instructor/lessons/form";
        }

        try {
            Lesson updated = lessonService.updateLesson(currentUser, lessonId, lessonForm);
            redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật bài học.");
            return "redirect:/instructor/courses/" + updated.getCourse().getCourseId() + "/lessons";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("course", course);
            model.addAttribute("sections", lessonService.listSections(currentUser, courseId));
            model.addAttribute("lessonId", lessonId);
            model.addAttribute("isEdit", true);
            model.addAttribute("currentTab", "lessons");
            model.addAttribute("errorMessage", ex.getMessage());
            return "instructor/lessons/form";
        }
    }

    // Xem truoc bai hoc va tai lieu
    @GetMapping("/instructor/courses/{courseId}/lessons/{lessonId}/preview")
    public String previewLesson(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long courseId,
            @PathVariable Long lessonId,
            Model model) {
        Course course = lessonService.getManageableCourse(currentUser, courseId);
        Lesson lesson = lessonService.getManageableLesson(currentUser, lessonId);

        model.addAttribute("course", course);
        model.addAttribute("lesson", lesson);
        model.addAttribute("materials", materialService.listMaterialsByLesson(lessonId));
        model.addAttribute("currentTab", "lessons");
        return "instructor/lessons/preview";
    }

    // Chuyen bai hoc sang trang thai INACTIVE
    @PostMapping("/instructor/courses/{courseId}/lessons/{lessonId}/deactivate")
    public String deactivateLesson(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long courseId,
            @PathVariable Long lessonId,
            RedirectAttributes redirectAttributes) {
        lessonService.deactivateLesson(currentUser, lessonId);
        redirectAttributes.addFlashAttribute(
                "successMessage", "Đã chuyển bài học sang trạng thái ngừng sử dụng.");
        return "redirect:/instructor/courses/" + courseId + "/lessons";
    }

    // Xoa bai hoc
    @PostMapping("/instructor/courses/{courseId}/lessons/{lessonId}/delete")
    public String deleteLesson(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long courseId,
            @PathVariable Long lessonId,
            RedirectAttributes redirectAttributes) {
        try {
            lessonService.deleteLesson(currentUser, courseId, lessonId);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa bài học.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/instructor/courses/" + courseId + "/lessons";
    }

    // ==================== QUAN LY PHAN HOC (SECTION) ====================

    @GetMapping("/instructor/courses/{courseId}/sections/new")
    public String showCreateSectionForm(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long courseId,
            Model model) {
        Course course = lessonService.getManageableCourse(currentUser, courseId);
        CourseSection section = new CourseSection();
        section.setSectionOrder(lessonService.nextSectionOrder(currentUser, courseId));

        model.addAttribute("course", course);
        model.addAttribute("section", section);
        model.addAttribute("isEdit", false);
        model.addAttribute("currentTab", "lessons");
        return "instructor/sections/form";
    }

    @PostMapping("/instructor/courses/{courseId}/sections")
    public String createSection(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long courseId,
            @RequestParam String sectionTitle,
            @RequestParam Integer sectionOrder,
            Model model,
            RedirectAttributes redirectAttributes) {
        try {
            lessonService.createSection(currentUser, courseId, sectionTitle, sectionOrder);
            redirectAttributes.addFlashAttribute("successMessage", "Đã tạo phần học mới.");
            return "redirect:/instructor/courses/" + courseId + "/lessons";
        } catch (IllegalArgumentException ex) {
            Course course = lessonService.getManageableCourse(currentUser, courseId);
            CourseSection section = new CourseSection();
            section.setSectionTitle(sectionTitle);
            section.setSectionOrder(sectionOrder);

            model.addAttribute("course", course);
            model.addAttribute("section", section);
            model.addAttribute("isEdit", false);
            model.addAttribute("currentTab", "lessons");
            model.addAttribute("errorMessage", ex.getMessage());
            return "instructor/sections/form";
        }
    }

    @GetMapping("/instructor/courses/{courseId}/sections/{sectionId}/edit")
    public String showEditSectionForm(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long courseId,
            @PathVariable Long sectionId,
            Model model) {
        Course course = lessonService.getManageableCourse(currentUser, courseId);
        CourseSection section = lessonService.getSection(currentUser, courseId, sectionId);

        model.addAttribute("course", course);
        model.addAttribute("section", section);
        model.addAttribute("sectionId", sectionId);
        model.addAttribute("isEdit", true);
        model.addAttribute("currentTab", "lessons");
        return "instructor/sections/form";
    }

    @PostMapping("/instructor/courses/{courseId}/sections/{sectionId}/edit")
    public String updateSection(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long courseId,
            @PathVariable Long sectionId,
            @RequestParam String sectionTitle,
            @RequestParam Integer sectionOrder,
            Model model,
            RedirectAttributes redirectAttributes) {
        try {
            lessonService.updateSection(currentUser, courseId, sectionId, sectionTitle, sectionOrder);
            redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật phần học.");
            return "redirect:/instructor/courses/" + courseId + "/lessons";
        } catch (IllegalArgumentException ex) {
            Course course = lessonService.getManageableCourse(currentUser, courseId);
            CourseSection section = new CourseSection();
            section.setSectionTitle(sectionTitle);
            section.setSectionOrder(sectionOrder);

            model.addAttribute("course", course);
            model.addAttribute("section", section);
            model.addAttribute("sectionId", sectionId);
            model.addAttribute("isEdit", true);
            model.addAttribute("currentTab", "lessons");
            model.addAttribute("errorMessage", ex.getMessage());
            return "instructor/sections/form";
        }
    }

    @PostMapping("/instructor/courses/{courseId}/sections/{sectionId}/delete")
    public String deleteSection(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long courseId,
            @PathVariable Long sectionId,
            RedirectAttributes redirectAttributes) {
        try {
            lessonService.deleteSection(currentUser, courseId, sectionId);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa phần học.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/instructor/courses/" + courseId + "/lessons";
    }
}
