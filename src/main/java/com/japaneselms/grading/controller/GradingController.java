package com.japaneselms.grading.controller;

import com.japaneselms.entity.Submission;
import com.japaneselms.grading.dto.GradeForm;
import com.japaneselms.grading.service.GradingService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class GradingController {
    private final GradingService service;
    public GradingController(GradingService service) { this.service = service; }

    @GetMapping("/instructor/grading")
    public String gradingList(Authentication auth, @RequestParam(defaultValue = "all") String status,
                              @RequestParam(defaultValue = "0") int page, Model model) {
        Page<Submission> results = service.instructorSubmissions(auth.getName(), status, service.page(page, 10));
        model.addAttribute("results", results);
        model.addAttribute("status", status);
        return "grading/instructor-list";
    }

    @GetMapping("/instructor/grading/{id}")
    public String gradeForm(Authentication auth, @PathVariable Long id, Model model) {
        Submission submission = service.gradingDetail(auth.getName(), id);
        GradeForm form = new GradeForm();
        form.setGrade(submission.getGrade());
        form.setFeedback(submission.getFeedback());
        model.addAttribute("submission", submission);
        model.addAttribute("gradeForm", form);
        return "grading/grade-form";
    }

    @PostMapping("/instructor/grading/{id}")
    public String save(Authentication auth, @PathVariable Long id,
                       @Valid @ModelAttribute("gradeForm") GradeForm form, BindingResult errors,
                       Model model, RedirectAttributes redirect) {
        if (errors.hasErrors()) {
            model.addAttribute("submission", service.gradingDetail(auth.getName(), id));
            return "grading/grade-form";
        }
        service.saveGrade(auth.getName(), id, form);
        redirect.addFlashAttribute("message", "Đã lưu điểm và nhận xét");
        return "redirect:/instructor/grading";
    }

    @GetMapping("/student/grades")
    public String myGrades(Authentication auth, @RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("results", service.studentGrades(auth.getName(), service.page(page, 10)));
        return "grading/student-grades";
    }

    @GetMapping({"/admin/results", "/admin/assessment-results"})
    public String adminResults(Authentication auth, @RequestParam(required = false) Long courseId,
                               @RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("results", service.adminResults(auth.getName(), courseId, service.page(page, 10)));
        model.addAttribute("courseId", courseId);
        model.addAttribute("isAdmin", true);
        model.addAttribute("currentTab", "assessment-results");
        return "grading/assessment-results";
    }

    @GetMapping({"/instructor/results", "/instructor/assessment-results"})
    public String instructorResults(Authentication auth, @RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("results", service.instructorSubmissions(auth.getName(), "graded", service.page(page, 10)));
        model.addAttribute("courseId", null);
        model.addAttribute("isAdmin", false);
        model.addAttribute("currentTab", "assessment-results");
        return "grading/assessment-results";
    }

    @GetMapping({
            "/instructor/results/{id}",
            "/instructor/assessment-results/{id}",
            "/student/results/{id}",
            "/admin/results/{id}",
            "/admin/assessment-results/{id}"
    })
    public String resultDetail(Authentication auth, @PathVariable Long id, Model model) {
        model.addAttribute("submission", service.detail(auth.getName(), id));
        return "grading/result-detail";
    }
}
