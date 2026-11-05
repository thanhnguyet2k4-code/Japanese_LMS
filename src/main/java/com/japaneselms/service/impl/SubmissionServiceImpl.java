package com.japaneselms.service.impl;

import com.japaneselms.dto.request.SubmissionCreateRequest;
import com.japaneselms.dto.response.SubmissionResponse;
import com.japaneselms.entity.Assignment;
import com.japaneselms.entity.Submission;
import com.japaneselms.entity.User;
import com.japaneselms.exception.AccessDeniedCourseException;
import com.japaneselms.exception.BusinessException;
import com.japaneselms.exception.ResourceNotFoundException;
import com.japaneselms.repository.AssignmentRepository;
import com.japaneselms.repository.EnrollmentRepository;
import com.japaneselms.repository.SubmissionRepository;
import com.japaneselms.repository.UserRepository;
import com.japaneselms.service.FileStorageService;
import com.japaneselms.service.SubmissionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

@Service
@Transactional
public class SubmissionServiceImpl implements SubmissionService {

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
            throw new AccessDeniedCourseException("Bạn chưa ghi danh vào khóa học chứa bài tập này.");
        }

        // PRE-2: Bài tập phải ở trạng thái ACTIVE
        if (!"ACTIVE".equalsIgnoreCase(assignment.getAssignmentStatus())) {
            throw new BusinessException("Bài tập này hiện đã ngừng nhận bài (trạng thái INACTIVE).");
        }

        // PRE-3 & BR-06: Mỗi sinh viên chỉ được nộp bài đúng 1 lần cho mỗi bài tập
        boolean alreadySubmitted = submissionRepository.existsByAssignment_AssignmentIdAndStudent_UserId(
                assignment.getAssignmentId(), studentId
        );
        if (alreadySubmitted) {
            throw new BusinessException("Bạn đã nộp bài này rồi. Hệ thống chỉ cho phép nộp bài một lần duy nhất.");
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
            } else if ("INSTRUCTOR".equalsIgnoreCase(user.getRole())) {
                if (!submission.getAssignment().getCourse().getInstructor().getUserId().equals(currentUserId)) {
                    throw new AccessDeniedCourseException("Bạn không có quyền xem bài nộp của khóa học này.");
                }
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

            if (!"ADMIN".equalsIgnoreCase(user.getRole())) {
                if (!"INSTRUCTOR".equalsIgnoreCase(user.getRole()) ||
                        !assignment.getCourse().getInstructor().getUserId().equals(currentUserId)) {
                    throw new AccessDeniedCourseException("Bạn không có quyền xem danh sách bài nộp của bài tập này.");
                }
            }
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
            } else if ("INSTRUCTOR".equalsIgnoreCase(user.getRole())) {
                if (!submission.getAssignment().getCourse().getInstructor().getUserId().equals(currentUserId)) {
                    throw new AccessDeniedCourseException("Bạn không có quyền tải tệp bài nộp của khóa học này.");
                }
            }
        }

        if (submission.getStoragePath() == null || submission.getStoragePath().isBlank()) {
            throw new BusinessException("Bài nộp này không có tệp đính kèm.");
        }

        return fileStorageService.loadFileAsResource(submission.getStoragePath());
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
        return dto;
    }
}
