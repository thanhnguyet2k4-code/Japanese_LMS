package com.japaneselms.grading.controller;

import com.japaneselms.entity.Submission;
import com.japaneselms.grading.dto.GradeForm;
import com.japaneselms.grading.service.GradingService;
import com.japaneselms.repository.SubmissionRepository;
import java.util.List;
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
    private final SubmissionRepository submissionRepository;

    public GradingController(GradingService service, SubmissionRepository submissionRepository) {
        this.service = service;
        this.submissionRepository = submissionRepository;
    }

    // Dữ liệu cho thanh chuyển nhanh giữa các bài nộp cùng bài tập
    private void addNavigation(Model model, Submission submission) {
        List<Submission> all = submissionRepository.findByAssignment_AssignmentIdOrderBySubmittedAtAsc(
                submission.getAssignment().getAssignmentId());
        int index = 0;
        Long prevId = null, nextId = null;
        String prevName = null, nextName = null;
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).getSubmissionId().equals(submission.getSubmissionId())) {
                index = i;
                if (i > 0) {
                    prevId = all.get(i - 1).getSubmissionId();
                    prevName = all.get(i - 1).getStudent().getFullName();
                }
                if (i < all.size() - 1) {
                    nextId = all.get(i + 1).getSubmissionId();
                    nextName = all.get(i + 1).getStudent().getFullName();
                }
                break;
            }
        }
        model.addAttribute("allSubmissions", all);
        model.addAttribute("currentIndex", index);
        model.addAttribute("totalSubmissions", all.size());
        model.addAttribute("prevId", prevId);
        model.addAttribute("nextId", nextId);
        model.addAttribute("prevStudentName", prevName);
        model.addAttribute("nextStudentName", nextName);
    }

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
        addNavigation(model, submission);
        return "grading/grade-form";
    }

    @PostMapping("/instructor/grading/{id}")
    public String save(Authentication auth, @PathVariable Long id,
                       @Valid @ModelAttribute("gradeForm") GradeForm form, BindingResult errors,
                       Model model, RedirectAttributes redirect) {
        if (errors.hasErrors()) {
            Submission current = service.gradingDetail(auth.getName(), id);
            model.addAttribute("submission", current);
            addNavigation(model, current);
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
