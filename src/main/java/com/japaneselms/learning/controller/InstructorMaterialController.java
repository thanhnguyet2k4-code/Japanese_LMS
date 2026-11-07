package com.japaneselms.learning.controller;

import com.japaneselms.entity.Lesson;
import com.japaneselms.entity.User;
import com.japaneselms.learning.dto.MaterialForm;
import com.japaneselms.learning.service.LearningMaterialService;
import com.japaneselms.learning.service.LessonService;
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
public class InstructorMaterialController {

    private final LearningMaterialService materialService;
    private final LessonService lessonService;

    public InstructorMaterialController(
            LearningMaterialService materialService,
            LessonService lessonService) {
        this.materialService = materialService;
        this.lessonService = lessonService;
    }

    // S12: Hien thi trang quan ly tai lieu cua bai hoc
    @GetMapping("/instructor/lessons/{lessonId}/materials")
    public String manageMaterials(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long lessonId,
            Model model) {
        loadMaterialPageData(currentUser, lessonId, model);
        model.addAttribute("materialForm", new MaterialForm());
        model.addAttribute("isEdit", false);
        return "instructor/lessons/materials";
    }

    // S12: Hien thi form sua tai lieu
    @GetMapping("/instructor/lessons/{lessonId}/materials/{materialId}/edit")
    public String showEditForm(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long lessonId,
            @PathVariable Long materialId,
            Model model) {
        loadMaterialPageData(currentUser, lessonId, model);
        model.addAttribute("materialForm",
                materialService.getMaterialForm(currentUser, lessonId, materialId));
        model.addAttribute("isEdit", true);
        model.addAttribute("materialId", materialId);
        return "instructor/lessons/materials";
    }

    // S12: Xu ly them tai lieu moi
    @PostMapping("/instructor/lessons/{lessonId}/materials")
    public String addMaterial(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long lessonId,
            @Valid @ModelAttribute("materialForm") MaterialForm materialForm,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            loadMaterialPageData(currentUser, lessonId, model);
            model.addAttribute("isEdit", false);
            return "instructor/lessons/materials";
        }

        try {
            materialService.addMaterial(currentUser, lessonId, materialForm);
            redirectAttributes.addFlashAttribute("successMessage", "Đã thêm tài liệu học tập.");
            return "redirect:/instructor/lessons/" + lessonId + "/materials";
        } catch (IllegalArgumentException ex) {
            loadMaterialPageData(currentUser, lessonId, model);
            model.addAttribute("isEdit", false);
            model.addAttribute("errorMessage", ex.getMessage());
            return "instructor/lessons/materials";
        }
    }

    // S12: Xu ly cap nhat tai lieu
    @PostMapping("/instructor/lessons/{lessonId}/materials/{materialId}/edit")
    public String updateMaterial(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long lessonId,
            @PathVariable Long materialId,
            @Valid @ModelAttribute("materialForm") MaterialForm materialForm,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            loadMaterialPageData(currentUser, lessonId, model);
            model.addAttribute("isEdit", true);
            model.addAttribute("materialId", materialId);
            return "instructor/lessons/materials";
        }

        try {
            materialService.updateMaterial(currentUser, lessonId, materialId, materialForm);
            redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật tài liệu.");
            return "redirect:/instructor/lessons/" + lessonId + "/materials";
        } catch (IllegalArgumentException ex) {
            loadMaterialPageData(currentUser, lessonId, model);
            model.addAttribute("isEdit", true);
            model.addAttribute("materialId", materialId);
            model.addAttribute("errorMessage", ex.getMessage());
            return "instructor/lessons/materials";
        }
    }

    // S12: Xoa tai lieu
    @PostMapping("/instructor/materials/{materialId}/delete")
    public String deleteMaterial(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long materialId,
            RedirectAttributes redirectAttributes) {
        Long lessonId = materialService.deleteMaterial(currentUser, materialId);
        redirectAttributes.addFlashAttribute("successMessage", "Đã xóa tài liệu.");
        return "redirect:/instructor/lessons/" + lessonId + "/materials";
    }

    // Tai hoac xem file PDF / MP3 (co kiem tra quyen truy cap cua Instructor/Student)
    @GetMapping("/files/materials/{materialId}")
    public ResponseEntity<Resource> downloadMaterial(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long materialId) {
        return materialService.downloadForUser(currentUser, materialId);
    }

    private void loadMaterialPageData(User currentUser, Long lessonId, Model model) {
        Lesson lesson = lessonService.getManageableLesson(currentUser, lessonId);
        model.addAttribute("lesson", lesson);
        model.addAttribute("course", lesson.getCourse());
        model.addAttribute("materials", materialService.listMaterialsByLesson(lessonId));
        model.addAttribute("currentTab", "lessons");
    }
}
