package com.japaneselms.course.service;

import com.japaneselms.course.dto.CourseForm;
import com.japaneselms.course.dto.CourseSummaryDto;
import com.japaneselms.course.exception.CourseNotFoundException;
import com.japaneselms.course.exception.DuplicateCourseNameException;
import com.japaneselms.entity.Course;
import com.japaneselms.entity.User;
import com.japaneselms.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * UC08 – Instructor quản lý khóa học của chính mình (tạo / xem / sửa / ngừng hoạt động).
 * instructorId luôn do Controller lấy từ SecurityContext, KHÔNG bao giờ nhận từ request.
 */
@Service
public class InstructorCourseService {

    /**
     * Kết quả của thao tác "Ngừng hoạt động" để Controller chọn đúng thông báo.
     */
    public enum DeactivateResult {
        DEACTIVATED,        // có dữ liệu liên quan -> chuyển INACTIVE (Soft Delete)
        DELETED,            // chưa có dữ liệu liên quan -> xóa hẳn
        ALREADY_INACTIVE    // đã INACTIVE từ trước -> không làm gì
    }

    private static final Set<String> SORTABLE_PROPERTIES = Set.of("courseName", "createdAt");
    private static final Sort DEFAULT_SORT = Sort.by(Sort.Direction.DESC, "createdAt");

    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final LessonRepository lessonRepository;
    private final AssignmentRepository assignmentRepository;
    private final UserRepository userRepository;

    public InstructorCourseService(CourseRepository courseRepository,
                                   EnrollmentRepository enrollmentRepository,
                                   LessonRepository lessonRepository,
                                   AssignmentRepository assignmentRepository,
                                   UserRepository userRepository) {
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.lessonRepository = lessonRepository;
        this.assignmentRepository = assignmentRepository;
        this.userRepository = userRepository;
    }

    // ===================== Luồng 1: danh sách khóa học của tôi =====================

    @Transactional(readOnly = true)
    public Page<CourseSummaryDto> getMyCourses(Long instructorId, Pageable pageable) {
        Pageable safePageable = PageableUtils.sanitize(pageable, SORTABLE_PROPERTIES, DEFAULT_SORT);

        return courseRepository
                .findByInstructor_UserId(instructorId, safePageable)
                .map(CourseSummaryDto::from);
    }

    // ===================== Luồng 2: tạo khóa học =====================

    @Transactional
    public Long createCourse(CourseForm form, Long instructorId) {
        // Đọc lại user từ DB: phiên đăng nhập có thể đã cũ (tài khoản bị khóa sau khi login)
        User instructor = userRepository.findById(instructorId)
                .filter(user -> "INSTRUCTOR".equalsIgnoreCase(user.getRole()) && user.isActive())
                .orElseThrow(() -> new AccessDeniedException("Tài khoản không có quyền tạo khóa học."));

        String courseName = form.getCourseName().trim();

        if (courseRepository.existsByInstructor_UserIdAndCourseNameIgnoreCase(instructorId, courseName)) {
            throw new DuplicateCourseNameException();
        }

        Course course = new Course();
        course.setCourseName(courseName);
        course.setDescription(form.getDescription().trim());
        course.setJlptLevel(form.getJlptLevel());
        course.setInstructor(instructor);
        course.setCourseStatus(resolveStatus(form));   // Instructor chọn ACTIVE/INACTIVE (mặc định ACTIVE)
        // createdAt / updatedAt do @PrePersist của Entity tự gán

        Course savedCourse = courseRepository.save(course);
        return savedCourse.getCourseId();
    }

    // ===================== Luồng 3: xem + sửa khóa học =====================

    /**
     * Nạp dữ liệu khóa học (của chính mình) vào form để sửa.
     */
    @Transactional(readOnly = true)
    public CourseForm getCourseForm(Long courseId, Long instructorId) {
        Course course = getOwnedCourse(courseId, instructorId);

        CourseForm form = new CourseForm();
        form.setCourseName(course.getCourseName());
        form.setJlptLevel(course.getJlptLevel());
        form.setDescription(course.getDescription());
        form.setCourseStatus(course.getCourseStatus());
        return form;
    }

    @Transactional
    public void updateCourse(Long courseId, CourseForm form, Long instructorId) {
        Course course = getOwnedCourse(courseId, instructorId);

        String courseName = form.getCourseName().trim();

        if (courseRepository.existsByInstructor_UserIdAndCourseNameIgnoreCaseAndCourseIdNot(
                instructorId, courseName, courseId)) {
            throw new DuplicateCourseNameException();
        }

        // Chỉ cập nhật 3 trường được phép (BR6). Entity đang managed -> tự UPDATE khi commit,
        // @PreUpdate tự cập nhật updatedAt.
        course.setCourseName(courseName);
        course.setJlptLevel(form.getJlptLevel());
        course.setDescription(form.getDescription().trim());
        course.setCourseStatus(resolveStatus(form));
    }

    /**
     * Trạng thái khóa học do Instructor chọn trên form; thiếu/không hợp lệ thì mặc định ACTIVE.
     */
    private String resolveStatus(CourseForm form) {
        String status = form.getCourseStatus();
        if (status != null && CourseStatus.INACTIVE.equalsIgnoreCase(status.trim())) {
            return CourseStatus.INACTIVE;
        }
        return CourseStatus.ACTIVE;
    }

    // ===================== Luồng 4: ngừng hoạt động / xóa =====================

    /**
     * Quy tắc không xóa cứng (BR4):
     * - Đã có enrollments, lessons hoặc assignments -> CHỈ chuyển courseStatus = INACTIVE (Soft Delete).
     * - Chưa có dữ liệu liên quan nào              -> xóa hẳn bản ghi (theo Postconditions UC08).
     * <p>
     * flush() ép lệnh DELETE chạy ngay, để nếu vừa có người đăng ký (vi phạm khóa ngoại)
     * thì DataIntegrityViolationException nổi lên tại đây -> Controller bắt và gọi forceDeactivate (E6).
     */
    @Transactional
    public DeactivateResult deactivateOrDelete(Long courseId, Long instructorId) {
        Course course = getOwnedCourse(courseId, instructorId);

        if (CourseStatus.INACTIVE.equals(course.getCourseStatus())) {
            return DeactivateResult.ALREADY_INACTIVE;
        }

        boolean hasRelatedData = enrollmentRepository.existsByCourse_CourseId(courseId)
                || lessonRepository.existsByCourse_CourseId(courseId)
                || assignmentRepository.existsByCourse_CourseId(courseId);

        if (hasRelatedData) {
            course.setCourseStatus(CourseStatus.INACTIVE);
            return DeactivateResult.DEACTIVATED;
        }

        courseRepository.delete(course);
        courseRepository.flush();
        return DeactivateResult.DELETED;
    }

    /**
     * Chỉ chuyển INACTIVE, không bao giờ xóa. Chạy trong transaction MỚI (do Controller gọi
     * sau khi transaction của deactivateOrDelete đã rollback vì lỗi khóa ngoại).
     */
    @Transactional
    public void forceDeactivate(Long courseId, Long instructorId) {
        Course course = getOwnedCourse(courseId, instructorId);
        course.setCourseStatus(CourseStatus.INACTIVE);
    }

    // ===================== Kiểm tra quyền sở hữu (chống IDOR) =====================

    /**
     * - Không có khóa học           -> CourseNotFoundException (404)
     * - Khóa học của Instructor khác -> AccessDeniedException   (403)
     */
    private Course getOwnedCourse(Long courseId, Long instructorId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new CourseNotFoundException(courseId));

        if (!course.getInstructor().getUserId().equals(instructorId)) {
            throw new AccessDeniedException("Bạn không có quyền thao tác trên khóa học này.");
        }
        return course;
    }
}
