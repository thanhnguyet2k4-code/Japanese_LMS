package com.japaneselms.assignment.controller;

import com.japaneselms.assignment.dto.request.AssignmentSaveRequest;
import com.japaneselms.assignment.dto.response.AssignmentResponse;
import com.japaneselms.assignment.dto.response.SubmissionResponse;
import com.japaneselms.entity.Assignment;
import com.japaneselms.entity.Course;
import com.japaneselms.entity.Lesson;
import com.japaneselms.entity.User;
import com.japaneselms.repository.AssignmentRepository;
import com.japaneselms.repository.CourseRepository;
import com.japaneselms.repository.LessonRepository;
import com.japaneselms.repository.SubmissionRepository;
import com.japaneselms.repository.UserRepository;
import com.japaneselms.assignment.service.AssignmentService;
import com.japaneselms.assignment.service.FileStorageService;
import com.japaneselms.assignment.service.SubmissionService;
import com.japaneselms.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Controller
@RequestMapping("/instructor")
public class AssignmentInstructorController {

    private final AssignmentService assignmentService;
    private final SubmissionService submissionService;
    private final FileStorageService fileStorageService;
    private final CourseRepository courseRepository;
    private final LessonRepository lessonRepository;
    private final UserRepository userRepository;
    private final AssignmentRepository assignmentRepository;
    private final SubmissionRepository submissionRepository;
    private final SecurityUtils securityUtils;

    public AssignmentInstructorController(
            AssignmentService assignmentService,
            SubmissionService submissionService,
            FileStorageService fileStorageService,
            CourseRepository courseRepository,
            LessonRepository lessonRepository,
            UserRepository userRepository,
            AssignmentRepository assignmentRepository,
            SubmissionRepository submissionRepository,
            SecurityUtils securityUtils
    ) {
        this.assignmentService = assignmentService;
        this.submissionService = submissionService;
        this.fileStorageService = fileStorageService;
        this.courseRepository = courseRepository;
        this.lessonRepository = lessonRepository;
        this.userRepository = userRepository;
        this.assignmentRepository = assignmentRepository;
        this.submissionRepository = submissionRepository;
        this.securityUtils = securityUtils;
    }

    private Long resolveInstructorId(Long courseId, Long assignmentId, HttpServletRequest request) {
        Long currentId = securityUtils.getCurrentUserId();
        if (currentId == null) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Không xác định được giảng viên đăng nhập.");
        }
        return currentId;
    }

    // ==========================================
    // S14 (Instructor view): Danh sách bài tập
    // ==========================================
    @GetMapping("/courses/{courseId}/assignments")
    public String listAssignments(
            @PathVariable Long courseId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, defaultValue = "ALL") String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request,
            Model model
    ) {
        Long instructorId = resolveInstructorId(courseId, null, request);
        Course course = courseRepository.findById(courseId).orElse(null);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<AssignmentResponse> assignmentPage = assignmentService.getAssignmentsByCourseForInstructor(
                courseId, keyword, status, instructorId, pageable
        );

        model.addAttribute("course", course);
        model.addAttribute("assignments", assignmentPage);
        model.addAttribute("keyword", keyword);
        model.addAttribute("status", status);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", assignmentPage.getTotalPages());
        return "instructor/assignment-list";
    }

    // ==========================================
    // S15: Màn hình tạo mới bài tập
    // ==========================================
    @GetMapping("/courses/{courseId}/assignments/new")
    public String showCreateForm(@PathVariable Long courseId, Model model) {
        Course course = courseRepository.findById(courseId).orElse(null);
        List<Lesson> lessons = lessonRepository.findByCourse_CourseIdOrderByLessonOrderAsc(courseId);

        AssignmentSaveRequest saveRequest = new AssignmentSaveRequest();
        saveRequest.setCourseId(courseId);
        saveRequest.setAssignmentStatus("ACTIVE");

        model.addAttribute("course", course);
        model.addAttribute("lessons", lessons);
        model.addAttribute("assignmentForm", saveRequest);
        model.addAttribute("isEdit", false);
        return "instructor/assignment-form";
    }

    @PostMapping("/courses/{courseId}/assignments/new")
    public String createAssignment(
            @PathVariable Long courseId,
            @Valid @ModelAttribute("assignmentForm") AssignmentSaveRequest form,
            BindingResult bindingResult,
            HttpServletRequest request,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        Long instructorId = resolveInstructorId(courseId, null, request);
        form.setCourseId(courseId);

        if (bindingResult.hasErrors()) {
            Course course = courseRepository.findById(courseId).orElse(null);
            List<Lesson> lessons = lessonRepository.findByCourse_CourseIdOrderByLessonOrderAsc(courseId);
            model.addAttribute("course", course);
            model.addAttribute("lessons", lessons);
            model.addAttribute("isEdit", false);
            return "instructor/assignment-form";
        }

        try {
            assignmentService.createAssignment(form, instructorId);
            redirectAttributes.addFlashAttribute("successMessage", "Tạo bài tập mới thành công!");
            return "redirect:/instructor/courses/" + courseId + "/assignments";
        } catch (Exception ex) {
            Course course = courseRepository.findById(courseId).orElse(null);
            List<Lesson> lessons = lessonRepository.findByCourse_CourseIdOrderByLessonOrderAsc(courseId);
            model.addAttribute("course", course);
            model.addAttribute("lessons", lessons);
            model.addAttribute("isEdit", false);
            model.addAttribute("errorMessage", ex.getMessage());
            return "instructor/assignment-form";
        }
    }

    // ==========================================
    // S15: Màn hình chỉnh sửa bài tập
    // ==========================================
    @GetMapping("/assignments/{assignmentId}/edit")
    public String showEditForm(@PathVariable Long assignmentId, HttpServletRequest request, Model model) {
        Long instructorId = resolveInstructorId(null, assignmentId, request);
        AssignmentSaveRequest form = assignmentService.getAssignmentForEdit(assignmentId, instructorId);
        Course course = courseRepository.findById(form.getCourseId()).orElse(null);
        List<Lesson> lessons = lessonRepository.findByCourse_CourseIdOrderByLessonOrderAsc(form.getCourseId());

        model.addAttribute("course", course);
        model.addAttribute("lessons", lessons);
        model.addAttribute("assignmentForm", form);
        model.addAttribute("isEdit", true);
        return "instructor/assignment-form";
    }

    @PostMapping("/assignments/{assignmentId}/edit")
    public String updateAssignment(
            @PathVariable Long assignmentId,
            @Valid @ModelAttribute("assignmentForm") AssignmentSaveRequest form,
            BindingResult bindingResult,
            HttpServletRequest request,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        Long instructorId = resolveInstructorId(form.getCourseId(), assignmentId, request);
        if (bindingResult.hasErrors()) {
            Course course = courseRepository.findById(form.getCourseId()).orElse(null);
            List<Lesson> lessons = lessonRepository.findByCourse_CourseIdOrderByLessonOrderAsc(form.getCourseId());
            model.addAttribute("course", course);
            model.addAttribute("lessons", lessons);
            model.addAttribute("isEdit", true);
            return "instructor/assignment-form";
        }

        try {
            assignmentService.updateAssignment(assignmentId, form, instructorId);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật bài tập thành công!");
            return "redirect:/instructor/courses/" + form.getCourseId() + "/assignments";
        } catch (Exception ex) {
            Course course = courseRepository.findById(form.getCourseId()).orElse(null);
            List<Lesson> lessons = lessonRepository.findByCourse_CourseIdOrderByLessonOrderAsc(form.getCourseId());
            model.addAttribute("course", course);
            model.addAttribute("lessons", lessons);
            model.addAttribute("isEdit", true);
            model.addAttribute("errorMessage", ex.getMessage());
            return "instructor/assignment-form";
        }
    }

    // Ngừng kích hoạt (Soft delete)
    @PostMapping("/assignments/{assignmentId}/deactivate")
    public String deactivateAssignment(
            @PathVariable Long assignmentId,
            @RequestParam Long courseId,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes
    ) {
        Long instructorId = resolveInstructorId(courseId, assignmentId, request);
        try {
            assignmentService.deactivateAssignment(assignmentId, instructorId);
            redirectAttributes.addFlashAttribute("successMessage", "Bài tập đã được chuyển sang trạng thái ngừng hoạt động (INACTIVE).");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/instructor/courses/" + courseId + "/assignments";
    }

    // Kích hoạt lại
    @PostMapping("/assignments/{assignmentId}/activate")
    public String activateAssignment(
            @PathVariable Long assignmentId,
            @RequestParam Long courseId,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes
    ) {
        Long instructorId = resolveInstructorId(courseId, assignmentId, request);
        try {
            assignmentService.activateAssignment(assignmentId, instructorId);
            redirectAttributes.addFlashAttribute("successMessage", "Kích hoạt bài tập thành công!");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/instructor/courses/" + courseId + "/assignments";
    }

    // ==========================================
    // S18: Review danh sách bài nộp của học viên
    // ==========================================
    @GetMapping("/assignments/{assignmentId}/submissions")
    public String reviewSubmissions(
            @PathVariable Long assignmentId,
            @RequestParam(required = false, defaultValue = "ALL") String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request,
            Model model
    ) {
        Long instructorId = resolveInstructorId(null, assignmentId, request);
        AssignmentResponse assignment = assignmentService.getAssignmentDetail(assignmentId, instructorId);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "submittedAt"));
        Page<SubmissionResponse> submissions = submissionService.getSubmissionsForAssignment(
                assignmentId, status, instructorId, pageable
        );

        model.addAttribute("assignment", assignment);
        model.addAttribute("submissions", submissions);
        model.addAttribute("status", status);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", submissions.getTotalPages());
        return "instructor/submission-review";
    }

    // Tải về file bài nộp an toàn (BR-11)
    @GetMapping("/submissions/{submissionId}/download")
    public ResponseEntity<Resource> downloadSubmissionFile(
            @PathVariable Long submissionId,
            HttpServletRequest request
    ) throws UnsupportedEncodingException {
        Long instructorId = resolveInstructorId(null, null, request);
        SubmissionResponse submission = submissionService.getSubmissionDetail(submissionId, instructorId);

        if (!submission.isHasFile()) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = submissionService.getSubmissionFileResource(submissionId, instructorId);

        String originalFilename = submission.getOriginalFileName() != null ? submission.getOriginalFileName() : "submission_file";
        String encodedFilename = URLEncoder.encode(originalFilename, StandardCharsets.UTF_8.toString()).replaceAll("\\+", "%20");

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodedFilename + "\"; filename*=UTF-8''" + encodedFilename)
                .body(resource);
    }

    // ==========================================
    // S20: Màn hình Chấm điểm bài nộp của học viên (Có điều hướng / chuyển bài nộp)
    // ==========================================
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    @GetMapping("/submissions/{submissionId}/grade")
    public String showGradeForm(
            @PathVariable Long submissionId,
            HttpServletRequest request,
            Model model
    ) {
        Long instructorId = resolveInstructorId(null, null, request);
        SubmissionResponse submission = submissionService.getSubmissionDetail(submissionId, instructorId);
        Long assignmentId = submission.getAssignmentId();

        List<com.japaneselms.entity.Submission> allSubs = submissionRepository.findByAssignment_AssignmentIdOrderBySubmittedAtAsc(assignmentId);
        int totalSubmissions = allSubs.size();
        int currentIndex = 0;
        Long prevId = null;
        Long nextId = null;
        String prevStudentName = null;
        String nextStudentName = null;

        for (int i = 0; i < allSubs.size(); i++) {
            if (allSubs.get(i).getSubmissionId().equals(submissionId)) {
                currentIndex = i;
                if (i > 0) {
                    prevId = allSubs.get(i - 1).getSubmissionId();
                    prevStudentName = allSubs.get(i - 1).getStudent().getFullName();
                }
                if (i < allSubs.size() - 1) {
                    nextId = allSubs.get(i + 1).getSubmissionId();
                    nextStudentName = allSubs.get(i + 1).getStudent().getFullName();
                }
                break;
            }
        }

        model.addAttribute("submission", submission);
        model.addAttribute("allSubmissions", allSubs);
        model.addAttribute("currentIndex", currentIndex);
        model.addAttribute("totalSubmissions", totalSubmissions);
        model.addAttribute("prevId", prevId);
        model.addAttribute("nextId", nextId);
        model.addAttribute("prevStudentName", prevStudentName);
        model.addAttribute("nextStudentName", nextStudentName);
        return "instructor/submission-grade";
    }

    @PostMapping("/submissions/{submissionId}/grade")
    public String saveGrade(
            @PathVariable Long submissionId,
            @RequestParam BigDecimal grade,
            @RequestParam(required = false) String feedback,
            @RequestParam(required = false, defaultValue = "save") String action,
            @RequestParam(required = false) Long nextId,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes
    ) {
        Long instructorId = resolveInstructorId(null, null, request);
        try {
            SubmissionResponse submission = submissionService.gradeSubmission(submissionId, grade, feedback, instructorId);
            redirectAttributes.addFlashAttribute("successMessage", "Đã lưu điểm cho học viên " + submission.getStudentName() + " (" + grade + " / 10 điểm)!");

            if ("saveAndNext".equals(action) && nextId != null) {
                return "redirect:/instructor/submissions/" + nextId + "/grade";
            }
            return "redirect:/instructor/assignments/" + submission.getAssignmentId() + "/submissions";
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/instructor/submissions/" + submissionId + "/grade";
        }
    }
}
