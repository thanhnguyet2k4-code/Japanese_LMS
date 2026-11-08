package com.japaneselms.course.service;

import com.japaneselms.course.dto.MyCourseDto;
import com.japaneselms.course.exception.CourseNotFoundException;
import com.japaneselms.course.exception.EnrollmentException;
import com.japaneselms.course.exception.EnrollmentException.Reason;
import com.japaneselms.entity.Course;
import com.japaneselms.entity.Enrollment;
import com.japaneselms.entity.User;
import com.japaneselms.repository.CourseRepository;
import com.japaneselms.repository.EnrollmentRepository;
import com.japaneselms.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * UC09 (đăng ký khóa học) + UC10 (khóa học của tôi).
 * studentId luôn do Controller lấy từ SecurityContext, không nhận từ request (chống IDOR).
 */
@Service
public class EnrollmentService {

    private static final Set<String> SORTABLE_PROPERTIES = Set.of("enrolledAt");
    private static final Sort DEFAULT_SORT = Sort.by(Sort.Direction.DESC, "enrolledAt");

    private final EnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;

    public EnrollmentService(EnrollmentRepository enrollmentRepository,
                             CourseRepository courseRepository,
                             UserRepository userRepository) {
        this.enrollmentRepository = enrollmentRepository;
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;
    }

    /**
     * UC07 – Student này đã đăng ký khóa học chưa? (quyết định nút hiển thị ở S07)
     */
    @Transactional(readOnly = true)
    public boolean isEnrolled(Long studentId, Long courseId) {
        return enrollmentRepository.existsByStudent_UserIdAndCourse_CourseId(studentId, courseId);
    }

    /**
     * UC09 – Đăng ký khóa học.
     * Kiểm tra theo thứ tự: tài khoản hợp lệ -> khóa học tồn tại -> ACTIVE -> chưa đăng ký.
     * Lớp bảo vệ cuối cùng là UNIQUE (student_id, course_id) trong DB: nếu hai request đến cùng lúc,
     * saveAndFlush ném DataIntegrityViolationException để Controller xử lý như "đã đăng ký" (E5).
     */
    @Transactional
    public void enroll(Long studentId, Long courseId) {
        // Đọc lại user từ DB thay vì tin dữ liệu trong session (có thể đã cũ)
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new AccessDeniedException("Không tìm thấy tài khoản đăng nhập."));

        boolean eligible = "STUDENT".equalsIgnoreCase(student.getRole())
                && "APPROVED".equalsIgnoreCase(student.getAccountStatus())
                && student.isActive();

        if (!eligible) {
            throw new EnrollmentException(Reason.ACCOUNT_NOT_ELIGIBLE,
                    "Tài khoản của bạn chưa đủ điều kiện để đăng ký khóa học.");
        }

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new CourseNotFoundException(courseId));

        if (!CourseStatus.ACTIVE.equals(course.getCourseStatus())) {
            throw new EnrollmentException(Reason.COURSE_INACTIVE, "Khóa học đã ngừng hoạt động.");
        }

        if (enrollmentRepository.existsByStudent_UserIdAndCourse_CourseId(studentId, courseId)) {
            throw new EnrollmentException(Reason.ALREADY_ENROLLED, "Bạn đã đăng ký khóa học này.");
        }

        Enrollment enrollment = new Enrollment();
        enrollment.setStudent(student);
        enrollment.setCourse(course);
        // enrolledAt do @PrePersist của Entity tự gán

        enrollmentRepository.saveAndFlush(enrollment);
    }

    /**
     * UC10 – Các khóa học đã đăng ký của chính Student, mới nhất trước, gồm cả khóa INACTIVE.
     */
    @Transactional(readOnly = true)
    public Page<MyCourseDto> getMyEnrollments(Long studentId, Pageable pageable) {
        Pageable safePageable = PageableUtils.sanitize(pageable, SORTABLE_PROPERTIES, DEFAULT_SORT);

        return enrollmentRepository
                .findByStudent_UserId(studentId, safePageable)
                .map(MyCourseDto::from);
    }
}
