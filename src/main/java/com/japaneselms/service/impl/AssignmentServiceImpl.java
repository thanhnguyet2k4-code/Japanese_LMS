package com.japaneselms.service.impl;

import com.japaneselms.dto.request.AssignmentSaveRequest;
import com.japaneselms.dto.response.AssignmentResponse;
import com.japaneselms.entity.Assignment;
import com.japaneselms.entity.Course;
import com.japaneselms.entity.Lesson;
import com.japaneselms.entity.Submission;
import com.japaneselms.entity.User;
import com.japaneselms.exception.AccessDeniedCourseException;
import com.japaneselms.exception.BusinessException;
import com.japaneselms.exception.ResourceNotFoundException;
import com.japaneselms.repository.*;
import com.japaneselms.service.AssignmentService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class AssignmentServiceImpl implements AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final CourseRepository courseRepository;
    private final LessonRepository lessonRepository;
    private final UserRepository userRepository;
    private final SubmissionRepository submissionRepository;
    private final EnrollmentRepository enrollmentRepository;

    public AssignmentServiceImpl(
            AssignmentRepository assignmentRepository,
            CourseRepository courseRepository,
            LessonRepository lessonRepository,
            UserRepository userRepository,
            SubmissionRepository submissionRepository,
            EnrollmentRepository enrollmentRepository
    ) {
        this.assignmentRepository = assignmentRepository;
        this.courseRepository = courseRepository;
        this.lessonRepository = lessonRepository;
        this.userRepository = userRepository;
        this.submissionRepository = submissionRepository;
        this.enrollmentRepository = enrollmentRepository;
    }

    @Override
    public AssignmentResponse createAssignment(AssignmentSaveRequest request, Long currentUserId) {
        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new ResourceNotFoundException("Khóa học không tồn tại với ID: " + request.getCourseId()));

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại với ID: " + currentUserId));

        // Kiểm tra quyền: chỉ Instructor của khóa học hoặc ADMIN
        validateInstructorCourseAccess(course, currentUser);

        Lesson lesson = null;
        if (request.getLessonId() != null) {
            lesson = lessonRepository.findById(request.getLessonId())
                    .orElseThrow(() -> new ResourceNotFoundException("Bài học không tồn tại với ID: " + request.getLessonId()));

            // BR-03: Lesson phải thuộc đúng Course
            if (!lesson.getCourse().getCourseId().equals(course.getCourseId())) {
                throw new BusinessException("Bài học đã chọn không thuộc khóa học này.");
            }
        }

        Assignment assignment = new Assignment();
        assignment.setCourse(course);
        assignment.setLesson(lesson);
        assignment.setTitle(request.getTitle().trim());
        assignment.setInstructions(request.getInstructions());
        assignment.setDueAt(request.getDueAt());
        assignment.setCreatedBy(currentUser);
        assignment.setAssignmentStatus("ACTIVE");

        Assignment saved = assignmentRepository.save(assignment);
        return mapToResponse(saved, null);
    }

    @Override
    public AssignmentResponse updateAssignment(Long assignmentId, AssignmentSaveRequest request, Long currentUserId) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Bài tập không tồn tại với ID: " + assignmentId));

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại với ID: " + currentUserId));

        validateInstructorCourseAccess(assignment.getCourse(), currentUser);

        Lesson lesson = null;
        if (request.getLessonId() != null) {
            lesson = lessonRepository.findById(request.getLessonId())
                    .orElseThrow(() -> new ResourceNotFoundException("Bài học không tồn tại với ID: " + request.getLessonId()));

            if (!lesson.getCourse().getCourseId().equals(assignment.getCourse().getCourseId())) {
                throw new BusinessException("Bài học đã chọn không thuộc khóa học này.");
            }
        }

        assignment.setTitle(request.getTitle().trim());
        assignment.setInstructions(request.getInstructions());
        assignment.setDueAt(request.getDueAt());
        assignment.setLesson(lesson);
        if (request.getAssignmentStatus() != null && !request.getAssignmentStatus().isBlank()) {
            assignment.setAssignmentStatus(request.getAssignmentStatus());
        }

        Assignment updated = assignmentRepository.save(assignment);
        return mapToResponse(updated, null);
    }

    @Override
    @Transactional(readOnly = true)
    public AssignmentResponse getAssignmentDetail(Long assignmentId, Long currentUserId) {
        Assignment assignment = assignmentRepository.findByIdWithDetails(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Bài tập không tồn tại với ID: " + assignmentId));

        if (currentUserId != null) {
            User user = userRepository.findById(currentUserId).orElse(null);
            if (user != null && "STUDENT".equalsIgnoreCase(user.getRole())) {
                // Kiểm tra sinh viên đã ghi danh chưa (BR-05)
                boolean isEnrolled = enrollmentRepository.existsByStudent_UserIdAndCourse_CourseId(
                        currentUserId, assignment.getCourse().getCourseId()
                );
                if (!isEnrolled) {
                    throw new AccessDeniedCourseException("Bạn chưa ghi danh vào khóa học này để xem bài tập.");
                }

                // BR-01: Sinh viên chỉ xem được bài tập ACTIVE
                if (!"ACTIVE".equalsIgnoreCase(assignment.getAssignmentStatus())) {
                    throw new BusinessException("Bài tập này hiện không khả dụng (đã ngừng kích hoạt).");
                }
            }
        }

        return mapToResponse(assignment, currentUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public AssignmentSaveRequest getAssignmentForEdit(Long assignmentId, Long currentUserId) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Bài tập không tồn tại với ID: " + assignmentId));

        if (currentUserId != null) {
            User currentUser = userRepository.findById(currentUserId)
                    .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại"));
            validateInstructorCourseAccess(assignment.getCourse(), currentUser);
        }

        AssignmentSaveRequest request = new AssignmentSaveRequest();
        request.setAssignmentId(assignment.getAssignmentId());
        request.setCourseId(assignment.getCourse().getCourseId());
        request.setLessonId(assignment.getLesson() != null ? assignment.getLesson().getLessonId() : null);
        request.setTitle(assignment.getTitle());
        request.setInstructions(assignment.getInstructions());
        request.setDueAt(assignment.getDueAt());
        request.setAssignmentStatus(assignment.getAssignmentStatus());
        return request;
    }

    @Override
    public void deactivateAssignment(Long assignmentId, Long currentUserId) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Bài tập không tồn tại với ID: " + assignmentId));

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại"));

        validateInstructorCourseAccess(assignment.getCourse(), currentUser);

        // BR-04: Tuyệt đối không xóa cứng bài tập đã phát sinh bài nộp; chỉ chuyển sang INACTIVE
        assignment.setAssignmentStatus("INACTIVE");
        assignmentRepository.save(assignment);
    }

    @Override
    public void activateAssignment(Long assignmentId, Long currentUserId) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Bài tập không tồn tại với ID: " + assignmentId));

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại"));

        validateInstructorCourseAccess(assignment.getCourse(), currentUser);

        assignment.setAssignmentStatus("ACTIVE");
        assignmentRepository.save(assignment);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AssignmentResponse> getAssignmentsByCourseForInstructor(
            Long courseId,
            String keyword,
            String status,
            Long currentUserId,
            Pageable pageable
    ) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Khóa học không tồn tại với ID: " + courseId));

        if (currentUserId != null) {
            User currentUser = userRepository.findById(currentUserId).orElse(null);
            if (currentUser != null) {
                validateInstructorCourseAccess(course, currentUser);
            }
        }

        String searchKeyword = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null;
        String searchStatus = (status != null && !status.trim().isEmpty() && !status.equalsIgnoreCase("ALL")) ? status.trim() : null;

        Page<Assignment> pageResult = assignmentRepository.searchAssignments(courseId, searchKeyword, searchStatus, pageable);
        return pageResult.map(a -> mapToResponse(a, currentUserId));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AssignmentResponse> getAssignmentsByCourseForStudent(
            Long courseId,
            Long currentUserId,
            Pageable pageable
    ) {
        // Kiểm tra xem sinh viên đã ghi danh vào khóa học chưa
        boolean isEnrolled = enrollmentRepository.existsByStudent_UserIdAndCourse_CourseId(currentUserId, courseId);
        if (!isEnrolled) {
            throw new AccessDeniedCourseException("Bạn chưa ghi danh vào khóa học này để xem danh sách bài tập.");
        }

        // BR-01: Sinh viên chỉ xem được các bài tập ACTIVE
        Page<Assignment> pageResult = assignmentRepository.findByCourse_CourseIdAndAssignmentStatus(courseId, "ACTIVE", pageable);
        return pageResult.map(a -> mapToResponse(a, currentUserId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssignmentResponse> getAllActiveAssignmentsByCourse(Long courseId) {
        return assignmentRepository.findByCourse_CourseId(courseId).stream()
                .filter(a -> "ACTIVE".equalsIgnoreCase(a.getAssignmentStatus()))
                .map(a -> mapToResponse(a, null))
                .collect(Collectors.toList());
    }

    private void validateInstructorCourseAccess(Course course, User user) {
        if ("ADMIN".equalsIgnoreCase(user.getRole())) {
            return;
        }
        if (!"INSTRUCTOR".equalsIgnoreCase(user.getRole()) || !course.getInstructor().getUserId().equals(user.getUserId())) {
            throw new AccessDeniedCourseException("Bạn không có quyền quản lý bài tập của khóa học này.");
        }
    }

    private AssignmentResponse mapToResponse(Assignment assignment, Long currentUserId) {
        AssignmentResponse dto = new AssignmentResponse();
        dto.setAssignmentId(assignment.getAssignmentId());
        dto.setCourseId(assignment.getCourse().getCourseId());
        dto.setCourseName(assignment.getCourse().getCourseName());
        if (assignment.getLesson() != null) {
            dto.setLessonId(assignment.getLesson().getLessonId());
            dto.setLessonTitle(assignment.getLesson().getLessonTitle());
        }
        dto.setTitle(assignment.getTitle());
        dto.setInstructions(assignment.getInstructions());
        dto.setDueAt(assignment.getDueAt());
        dto.setAssignmentStatus(assignment.getAssignmentStatus());
        dto.setCreatedAt(assignment.getCreatedAt());
        dto.setUpdatedAt(assignment.getUpdatedAt());
        if (assignment.getCreatedBy() != null) {
            dto.setCreatedByName(assignment.getCreatedBy().getFullName());
        }

        // Đếm tổng số bài nộp
        long count = submissionRepository.countByAssignment_AssignmentId(assignment.getAssignmentId());
        dto.setSubmissionCount(count);

        // Kiểm tra quá hạn
        if (assignment.getDueAt() != null) {
            dto.setOverdue(LocalDateTime.now().isAfter(assignment.getDueAt()));
        } else {
            dto.setOverdue(false);
        }

        // Kiểm tra xem sinh viên đã nộp chưa
        if (currentUserId != null) {
            Optional<Submission> submissionOpt = submissionRepository.findByAssignment_AssignmentIdAndStudent_UserId(
                    assignment.getAssignmentId(), currentUserId
            );
            if (submissionOpt.isPresent()) {
                dto.setHasSubmitted(true);
                dto.setStudentSubmissionId(submissionOpt.get().getSubmissionId());
            } else {
                dto.setHasSubmitted(false);
            }
        }

        return dto;
    }
}
