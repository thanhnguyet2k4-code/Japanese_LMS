package com.japaneselms.assignment.controller;

import com.japaneselms.assignment.dto.request.SubmissionCreateRequest;
import com.japaneselms.assignment.dto.response.AssignmentResponse;
import com.japaneselms.assignment.dto.response.SubmissionResponse;
import com.japaneselms.entity.Assignment;
import com.japaneselms.entity.Course;
import com.japaneselms.entity.Enrollment;
import com.japaneselms.entity.User;
import com.japaneselms.repository.AssignmentRepository;
import com.japaneselms.repository.CourseRepository;
import com.japaneselms.repository.EnrollmentRepository;
import com.japaneselms.repository.UserRepository;
import com.japaneselms.assignment.service.AssignmentService;
import com.japaneselms.assignment.service.SubmissionService;
import com.japaneselms.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Controller
@RequestMapping("/student")
public class AssignmentStudentController {

    private final AssignmentService assignmentService;
    private final SubmissionService submissionService;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final AssignmentRepository assignmentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final SecurityUtils securityUtils;

    public AssignmentStudentController(
            AssignmentService assignmentService,
            SubmissionService submissionService,
            CourseRepository courseRepository,
            UserRepository userRepository,
            AssignmentRepository assignmentRepository,
            EnrollmentRepository enrollmentRepository,
            SecurityUtils securityUtils
    ) {
        this.assignmentService = assignmentService;
        this.submissionService = submissionService;
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;
        this.assignmentRepository = assignmentRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.securityUtils = securityUtils;
    }

    private Long resolveStudentId(Long courseId, Long assignmentId, HttpServletRequest request) {
        Long currentId = securityUtils.getCurrentUserId();
        if (currentId == null) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Không xác định được học viên đăng nhập.");
        }
        return currentId;
    }

    // ==========================================
    // S14 (Student view): Xem danh sách bài tập của khóa học
    // ==========================================
    @GetMapping("/courses/{courseId}/assignments")
    public String listAssignmentsForStudent(
            @PathVariable Long courseId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request,
            Model model
    ) {
        Long studentId = resolveStudentId(courseId, null, request);
        Course course = courseRepository.findById(courseId).orElse(null);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<AssignmentResponse> assignmentPage = assignmentService.getAssignmentsByCourseForStudent(
                courseId, studentId, pageable
        );

        model.addAttribute("course", course);
        model.addAttribute("assignments", assignmentPage);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", assignmentPage.getTotalPages());
        model.addAttribute("currentTab", "lessons");
        return "student/assignment-list";
    }

    // ==========================================
    // S16: Chi tiết bài tập và trạng thái nộp bài
    // ==========================================
    @GetMapping("/assignments/{assignmentId}")
    public String viewAssignmentDetail(@PathVariable Long assignmentId, HttpServletRequest request, Model model) {
        Long studentId = resolveStudentId(null, assignmentId, request);
        AssignmentResponse assignment = assignmentService.getAssignmentDetail(assignmentId, studentId);
        SubmissionResponse submission = submissionService.getStudentSubmission(assignmentId, studentId);

        model.addAttribute("assignment", assignment);
        model.addAttribute("submission", submission);
        model.addAttribute("currentTab", "lessons");
        return "student/assignment-detail";
    }

    // ==========================================
    // S17: Màn hình nộp bài tập
    // ==========================================
    @GetMapping("/assignments/{assignmentId}/submit")
    public String showSubmitForm(
            @PathVariable Long assignmentId,
            HttpServletRequest request,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        Long studentId = resolveStudentId(null, assignmentId, request);
        AssignmentResponse assignment = assignmentService.getAssignmentDetail(assignmentId, studentId);

        // Kiểm tra nếu đã nộp rồi thì không cho vào form nộp (BR-06)
        SubmissionResponse existingSubmission = submissionService.getStudentSubmission(assignmentId, studentId);
        if (existingSubmission != null && !existingSubmission.isReopened()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Bạn đã nộp bài này rồi. Bạn chỉ có thể nộp lại khi giảng viên mở lại bài tập.");
            return "redirect:/student/assignments/" + assignmentId;
        }

        SubmissionCreateRequest submitRequest = new SubmissionCreateRequest();
        submitRequest.setAssignmentId(assignmentId);
        if (existingSubmission != null && existingSubmission.isReopened()) {
            submitRequest.setResponseText(existingSubmission.getResponseText());
            model.addAttribute("existingSubmission", existingSubmission);
        }

        model.addAttribute("assignment", assignment);
        model.addAttribute("submissionForm", submitRequest);
        model.addAttribute("currentTab", "lessons");
        return "student/assignment-submit";
    }

    @PostMapping("/assignments/{assignmentId}/submit")
    public String submitAssignment(
            @PathVariable Long assignmentId,
            @ModelAttribute("submissionForm") SubmissionCreateRequest form,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes,
            Model model
    ) {
        Long studentId = resolveStudentId(null, assignmentId, request);
        form.setAssignmentId(assignmentId);

        try {
            submissionService.submitAssignment(form, studentId);
            redirectAttributes.addFlashAttribute("successMessage", "Nộp bài tập thành công!");
            return "redirect:/student/assignments/" + assignmentId;
        } catch (Exception ex) {
            AssignmentResponse assignment = assignmentService.getAssignmentDetail(assignmentId, studentId);
            SubmissionResponse existingSubmission = submissionService.getStudentSubmission(assignmentId, studentId);
            model.addAttribute("assignment", assignment);
            model.addAttribute("submissionForm", form);
            if (existingSubmission != null && existingSubmission.isReopened()) {
                model.addAttribute("existingSubmission", existingSubmission);
            }
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("currentTab", "lessons");
            return "student/assignment-submit";
        }
    }

    // Học viên tải về tệp bài nộp của chính mình
    @GetMapping("/submissions/{submissionId}/download")
    public ResponseEntity<Resource> downloadMySubmissionFile(
            @PathVariable Long submissionId,
            HttpServletRequest request
    ) throws UnsupportedEncodingException {
        Long studentId = resolveStudentId(null, null, request);
        SubmissionResponse submission = submissionService.getSubmissionDetail(submissionId, studentId);

        if (!submission.isHasFile()) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = submissionService.getSubmissionFileResource(submissionId, studentId);

        String originalFilename = submission.getOriginalFileName() != null ? submission.getOriginalFileName() : "my_submission";
        String encodedFilename = URLEncoder.encode(originalFilename, StandardCharsets.UTF_8.toString()).replaceAll("\\+", "%20");

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodedFilename + "\"; filename*=UTF-8''" + encodedFilename)
                .body(resource);
    }
}
