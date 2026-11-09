package com.japaneselms.grading.service;

import com.japaneselms.entity.Submission;
import com.japaneselms.entity.User;
import com.japaneselms.grading.dto.GradeForm;
import com.japaneselms.grading.repository.GradingRepository;
import com.japaneselms.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;

@Service
public class GradingService {
    private final GradingRepository gradingRepository;
    private final UserRepository userRepository;

    public GradingService(GradingRepository gradingRepository, UserRepository userRepository) {
        this.gradingRepository = gradingRepository;
        this.userRepository = userRepository;
    }

    private User currentUser(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new AccessDeniedException("Không tìm thấy tài khoản"));
    }

    private void requireRole(User user, String role) {
        if (!role.equalsIgnoreCase(user.getRole()) || !user.isActive()) {
            throw new AccessDeniedException("Không có quyền truy cập");
        }
    }

    public Pageable page(int page, int size) {
        return PageRequest.of(Math.max(0, page), Math.min(50, Math.max(1, size)),
                Sort.by(Sort.Direction.DESC, "submittedAt"));
    }

    @Transactional(readOnly = true)
    public Page<Submission> instructorSubmissions(String email, String status, Pageable pageable) {
        User instructor = currentUser(email);
        requireRole(instructor, "INSTRUCTOR");
        String safeStatus = ("pending".equals(status) || "graded".equals(status)) ? status : "all";
        return gradingRepository.findForInstructor(instructor.getUserId(), safeStatus, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Submission> studentGrades(String email, Pageable pageable) {
        User student = currentUser(email);
        requireRole(student, "STUDENT");
        return gradingRepository.findStudentGrades(student.getUserId(), pageable);
    }

    @Transactional(readOnly = true)
    public Page<Submission> adminResults(String email, Long courseId, Pageable pageable) {
        requireRole(currentUser(email), "ADMIN");
        return courseId == null ? gradingRepository.findAll(pageable)
                : gradingRepository.findAdminResultsForCourse(courseId, pageable);
    }

    @Transactional(readOnly = true)
    public Submission detail(String email, Long submissionId) {
        User user = currentUser(email);
        Submission s = find(submissionId);
        checkView(user, s);
        return s;
    }

    @Transactional(readOnly = true)
    public Submission gradingDetail(String email, Long submissionId) {
        User instructor = currentUser(email);
        requireRole(instructor, "INSTRUCTOR");
        Submission submission = find(submissionId);
        requireCourseInstructor(instructor, submission);
        return submission;
    }

    private void requireCourseInstructor(User instructor, Submission submission) {
        if (!submission.getAssignment().getCourse().getInstructor().getUserId()
                .equals(instructor.getUserId())) {
            throw new AccessDeniedException("Bạn không phụ trách khóa học này");
        }
    }

    @Transactional
    public void saveGrade(String email, Long submissionId, GradeForm form) {
        User instructor = currentUser(email);
        requireRole(instructor, "INSTRUCTOR");
        Submission submission = find(submissionId);
        requireCourseInstructor(instructor, submission);
        if (form.getGrade() == null || form.getGrade().compareTo(java.math.BigDecimal.ZERO) < 0
                || form.getGrade().compareTo(java.math.BigDecimal.TEN) > 0
                || form.getGrade().scale() > 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Điểm phải từ 0 đến 10, tối đa 1 số thập phân");
        }
        if (form.getFeedback() != null && form.getFeedback().length() > 2000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nhận xét quá dài");
        }
        submission.setGrade(form.getGrade());
        submission.setFeedback(form.getFeedback() == null ? null : form.getFeedback().trim());
        submission.setGradedBy(instructor);
        submission.setGradedAt(LocalDateTime.now());
        gradingRepository.save(submission);
    }

    private Submission find(Long id) {
        return gradingRepository.findDetailed(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy bài nộp"));
    }

    private void checkView(User user, Submission s) {
        if (!user.isActive()) throw new AccessDeniedException("Tài khoản không hoạt động");
        if ("ADMIN".equalsIgnoreCase(user.getRole())) return;
        if ("INSTRUCTOR".equalsIgnoreCase(user.getRole()) &&
                s.getAssignment().getCourse().getInstructor().getUserId().equals(user.getUserId())) return;
        if ("STUDENT".equalsIgnoreCase(user.getRole()) &&
                s.getStudent().getUserId().equals(user.getUserId()) && s.getGrade() != null) return;
        throw new AccessDeniedException("Bạn không được xem kết quả này");
    }
}
