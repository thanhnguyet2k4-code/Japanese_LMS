package com.japaneselms.assignment.service.impl;

import com.japaneselms.assignment.dto.request.SubmissionCreateRequest;
import com.japaneselms.assignment.dto.response.SubmissionResponse;
import com.japaneselms.entity.Assignment;
import com.japaneselms.entity.Enrollment;
import com.japaneselms.entity.Submission;
import com.japaneselms.entity.User;
import com.japaneselms.exception.AccessDeniedCourseException;
import com.japaneselms.exception.BusinessException;
import com.japaneselms.exception.ResourceNotFoundException;
import com.japaneselms.repository.AssignmentRepository;
import com.japaneselms.repository.EnrollmentRepository;
import com.japaneselms.repository.SubmissionRepository;
import com.japaneselms.repository.UserRepository;
import com.japaneselms.assignment.service.FileStorageService;
import com.japaneselms.assignment.service.SubmissionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Transactional
public class SubmissionServiceImpl implements SubmissionService {

    public static final int MAX_RESUBMISSIONS = 3;

    private final SubmissionRepository submissionRepository;
    private final AssignmentRepository assignmentRepository;
    private final UserRepository userRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final FileStorageService fileStorageService;

    public SubmissionServiceImpl(
            SubmissionRepository submissionRepository,
            AssignmentRepository assignmentRepository,
            UserRepository userRepository,
            EnrollmentRepository enrollmentRepository,
            FileStorageService fileStorageService
    ) {
        this.submissionRepository = submissionRepository;
        this.assignmentRepository = assignmentRepository;
        this.userRepository = userRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.fileStorageService = fileStorageService;
    }

    @Override
    public SubmissionResponse submitAssignment(SubmissionCreateRequest request, Long studentId) {
        Assignment assignment = assignmentRepository.findById(request.getAssignmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Bài tập không tồn tại với ID: " + request.getAssignmentId()));

        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Học viên không tồn tại với ID: " + studentId));

        // PRE-1: Kiểm tra sinh viên có được phê duyệt không
        if (!"APPROVED".equalsIgnoreCase(student.getAccountStatus()) || !student.isActive()) {
            throw new BusinessException("Tài khoản của bạn chưa được duyệt hoặc đang bị khóa.");
        }

        // Kiểm tra ghi danh khóa học
        boolean isEnrolled = enrollmentRepository.existsByStudent_UserIdAndCourse_CourseId(studentId, assignment.getCourse().getCourseId());
        if (!isEnrolled) {
            throw new AccessDeniedCourseException("Bạn chưa ghi danh vào khóa học của bài tập này.");
        }

        // PRE-2: Bài tập phải ở trạng thái ACTIVE
        if (!"ACTIVE".equalsIgnoreCase(assignment.getAssignmentStatus())) {
            throw new BusinessException("Bài tập này hiện đã ngừng nhận bài (trạng thái INACTIVE).");
        }

        // PRE-3 & BR-06: Kiểm tra bài nộp hiện có
        Optional<Submission> existingSubmissionOpt = submissionRepository.findByAssignment_AssignmentIdAndStudent_UserId(
                assignment.getAssignmentId(), studentId
        );

        if (existingSubmissionOpt.isPresent()) {
            Submission existingSubmission = existingSubmissionOpt.get();
            if (!existingSubmission.isReopened()) {
                throw new BusinessException("Bạn đã nộp bài này rồi. Bạn chỉ có thể nộp lại khi giảng viên mở lại bài tập.");
            }

            // Trường hợp reopened = true: cập nhật CHÍNH bản ghi cũ
            boolean hasNewText = request.getResponseText() != null && !request.getResponseText().trim().isEmpty();
            MultipartFile file = request.getFile();
            boolean hasNewFile = file != null && !file.isEmpty();
            boolean hasOldFile = existingSubmission.getStoragePath() != null && !existingSubmission.getStoragePath().isBlank();
            boolean hasEffectiveFile = hasNewFile || hasOldFile;

            if (!hasNewText && !hasEffectiveFile) {
                throw new BusinessException("Vui lòng nhập nội dung văn bản hoặc đính kèm tệp bài làm.");
            }

            if (hasNewFile) {
                fileStorageService.validateSubmissionFile(file);
                String newStoragePath = fileStorageService.storeFile(file);
                String oldStoragePath = existingSubmission.getStoragePath();
                if (oldStoragePath != null && !oldStoragePath.isBlank()) {
                    fileStorageService.deleteFile(oldStoragePath);
                }
                existingSubmission.setStoragePath(newStoragePath);
                existingSubmission.setOriginalFileName(file.getOriginalFilename());
                String fileType = null;
                if (file.getOriginalFilename() != null && file.getOriginalFilename().contains(".")) {
                    fileType = file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf(".") + 1).toUpperCase();
                }
                existingSubmission.setFileType(fileType);
            }

            existingSubmission.setResponseText(hasNewText ? request.getResponseText().trim() : null);
            existingSubmission.setSubmittedAt(LocalDateTime.now());
            existingSubmission.setGrade(null);
            existingSubmission.setFeedback(null);
            existingSubmission.setGradedBy(null);
            existingSubmission.setGradedAt(null);
            existingSubmission.setReopened(false);
            existingSubmission.setResubmitCount(existingSubmission.getResubmitCount() + 1);

            Submission saved = submissionRepository.save(existingSubmission);
            return mapToResponse(saved);
        }

        // BR-08: Bài nộp bắt buộc phải có ít nhất nội dung văn bản hoặc tệp đính kèm
        boolean hasText = request.getResponseText() != null && !request.getResponseText().trim().isEmpty();
        MultipartFile file = request.getFile();
        boolean hasFile = file != null && !file.isEmpty();

        if (!hasText && !hasFile) {
            throw new BusinessException("Vui lòng nhập nội dung văn bản hoặc đính kèm tệp bài làm.");
        }

        String storagePath = null;
        String originalFileName = null;
        String fileType = null;

        if (hasFile) {
            fileStorageService.validateSubmissionFile(file);
            storagePath = fileStorageService.storeFile(file);
            originalFileName = file.getOriginalFilename();
            if (originalFileName != null && originalFileName.contains(".")) {
                fileType = originalFileName.substring(originalFileName.lastIndexOf(".") + 1).toUpperCase();
            }
        }

        Submission submission = new Submission();
        submission.setAssignment(assignment);
        submission.setStudent(student);
        submission.setResponseText(hasText ? request.getResponseText().trim() : null);
        submission.setStoragePath(storagePath);
        submission.setOriginalFileName(originalFileName);
        submission.setFileType(fileType);
        submission.setSubmittedAt(LocalDateTime.now());
        submission.setGrade(null);
        submission.setFeedback(null);
        submission.setGradedBy(null);
        submission.setGradedAt(null);
        submission.setReopened(false);
        submission.setResubmitCount(0);

        Submission saved = submissionRepository.save(submission);
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public SubmissionResponse getSubmissionDetail(Long submissionId, Long currentUserId) {
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Bài nộp không tồn tại với ID: " + submissionId));

        if (currentUserId != null) {
            User user = userRepository.findById(currentUserId)
                    .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại"));

            if ("STUDENT".equalsIgnoreCase(user.getRole())) {
                if (!submission.getStudent().getUserId().equals(currentUserId)) {
                    throw new AccessDeniedCourseException("Bạn chỉ được xem bài nộp của chính mình.");
                }
            } else {
                checkStaffAccess(user, submission.getAssignment());
            }
        }

        return mapToResponse(submission);
    }

    @Override
    @Transactional(readOnly = true)
    public SubmissionResponse getStudentSubmission(Long assignmentId, Long studentId) {
        return submissionRepository.findByAssignment_AssignmentIdAndStudent_UserId(assignmentId, studentId)
                .map(this::mapToResponse)
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SubmissionResponse> getSubmissionsForAssignment(
            Long assignmentId,
            String gradingStatus,
            Long currentUserId,
            Pageable pageable
    ) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Bài tập không tồn tại với ID: " + assignmentId));

        if (currentUserId != null) {
            User user = userRepository.findById(currentUserId)
                    .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại"));

            checkStaffAccess(user, assignment);
        }

        String filterStatus = (gradingStatus != null && !gradingStatus.isBlank() && !gradingStatus.equalsIgnoreCase("ALL"))
                ? gradingStatus.trim().toUpperCase()
                : null;

        Page<Submission> pageResult = submissionRepository.findSubmissionsWithFilter(assignmentId, filterStatus, pageable);
        return pageResult.map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public org.springframework.core.io.Resource getSubmissionFileResource(Long submissionId, Long currentUserId) {
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Bài nộp không tồn tại với ID: " + submissionId));

        if (currentUserId != null) {
            User user = userRepository.findById(currentUserId)
                    .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại"));

            if ("STUDENT".equalsIgnoreCase(user.getRole())) {
                if (!submission.getStudent().getUserId().equals(currentUserId)) {
                    throw new AccessDeniedCourseException("Bạn chỉ được tải tệp bài nộp của chính mình.");
                }
            } else {
                checkStaffAccess(user, submission.getAssignment());
            }
        }

        if (submission.getStoragePath() == null || submission.getStoragePath().isBlank()) {
            throw new BusinessException("Bài nộp này không có tệp đính kèm.");
        }

        return fileStorageService.loadFileAsResource(submission.getStoragePath());
    }

    @Override
    public SubmissionResponse reopenSubmission(Long submissionId, Long instructorId) {
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Bài nộp không tồn tại với ID: " + submissionId));

        User user = userRepository.findById(instructorId)
                .orElseThrow(() -> new AccessDeniedCourseException("Không xác định được giảng viên."));
        // Giữ nguyên quy tắc phân quyền của nhóm: ADMIN hoặc đúng giảng viên phụ trách khóa học
        checkStaffAccess(user, submission.getAssignment());

        if (!"ACTIVE".equalsIgnoreCase(submission.getAssignment().getAssignmentStatus())) {
            throw new BusinessException("Không thể mở lại bài nộp vì bài tập đang ở trạng thái không hoạt động (INACTIVE).");
        }

        if (submission.isReopened()) {
            throw new BusinessException("Bài nộp này đã được mở lại, đang chờ học viên nộp lại.");
        }

        if (submission.getResubmitCount() >= MAX_RESUBMISSIONS) {
            throw new BusinessException("Học viên đã dùng hết " + MAX_RESUBMISSIONS + " lượt nộp lại.");
        }

        submission.setReopened(true);
        submission.setReopenedAt(LocalDateTime.now());
        Submission saved = submissionRepository.save(submission);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public SubmissionResponse gradeSubmission(Long submissionId, java.math.BigDecimal grade, String feedback, Long instructorId) {
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Bài nộp không tồn tại với ID: " + submissionId));

        if (grade == null || grade.compareTo(java.math.BigDecimal.ZERO) < 0 || grade.compareTo(java.math.BigDecimal.TEN) > 0) {
            throw new BusinessException("Điểm số không hợp lệ. Vui lòng nhập điểm trong thang từ 0.0 đến 10.0.");
        }

        User instructor = userRepository.findById(instructorId)
                .orElseThrow(() -> new AccessDeniedCourseException("Không xác định được giảng viên."));
        checkStaffAccess(instructor, submission.getAssignment());

        submission.setGrade(grade);
        submission.setFeedback(feedback != null && !feedback.isBlank() ? feedback.trim() : null);
        submission.setGradedBy(instructor);
        submission.setGradedAt(LocalDateTime.now());

        Submission saved = submissionRepository.save(submission);
        return mapToResponse(saved);
    }

    // ADMIN được phép; INSTRUCTOR chỉ được thao tác trên khóa học mình phụ trách
    private void checkStaffAccess(User user, Assignment assignment) {
        if ("ADMIN".equalsIgnoreCase(user.getRole())) {
            return;
        }
        if (!"INSTRUCTOR".equalsIgnoreCase(user.getRole())
                || assignment.getCourse() == null
                || assignment.getCourse().getInstructor() == null
                || !assignment.getCourse().getInstructor().getUserId().equals(user.getUserId())) {
            throw new AccessDeniedCourseException("Bạn không có quyền với bài nộp của khóa học này.");
        }
    }

    private SubmissionResponse mapToResponse(Submission s) {
        SubmissionResponse dto = new SubmissionResponse();
        dto.setSubmissionId(s.getSubmissionId());
        dto.setAssignmentId(s.getAssignment().getAssignmentId());
        dto.setAssignmentTitle(s.getAssignment().getTitle());
        dto.setStudentId(s.getStudent().getUserId());
        dto.setStudentName(s.getStudent().getFullName());
        dto.setStudentEmail(s.getStudent().getEmail());
        dto.setResponseText(s.getResponseText());
        dto.setOriginalFileName(s.getOriginalFileName());
        dto.setFileType(s.getFileType());
        dto.setSubmittedAt(s.getSubmittedAt());
        dto.setGrade(s.getGrade());
        dto.setFeedback(s.getFeedback());
        if (s.getGradedBy() != null) {
            dto.setGradedByName(s.getGradedBy().getFullName());
        }
        dto.setGradedAt(s.getGradedAt());
        dto.setGraded(s.getGrade() != null);
        dto.setHasFile(s.getStoragePath() != null && !s.getStoragePath().isBlank());
        dto.setReopened(s.isReopened());
        dto.setReopenedAt(s.getReopenedAt());
        dto.setResubmitCount(s.getResubmitCount());
        dto.setMaxResubmissions(MAX_RESUBMISSIONS);
        dto.setCanReopen(!s.isReopened() && s.getResubmitCount() < MAX_RESUBMISSIONS);
        return dto;
    }
}
