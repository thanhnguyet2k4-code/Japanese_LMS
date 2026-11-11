package com.japaneselms.course.service;

import com.japaneselms.common.ListPaging;
import com.japaneselms.course.dto.CourseLevel;
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

import java.util.ArrayList;
import java.util.List;
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

        // Khoa hoc chua co bai hoc nao dang mo thi chua cho hoc vien dang ky.
        if (courseRepository.findOpenForStudents(courseId).isEmpty()) {
            throw new EnrollmentException(Reason.COURSE_INACTIVE,
                    "Khóa học chưa sẵn sàng để đăng ký.");
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

    /**
     * UC10 (mở rộng) – Khóa học của tôi có tìm kiếm theo tên, lọc cấp độ / trạng thái,
     * sắp xếp và phân trang. Chỉ đọc dữ liệu của chính Student (studentId do Controller lấy từ session).
     * Không thay đổi getMyEnrollments(...) ở trên.
     */
    @Transactional(readOnly = true)
    public Page<MyCourseDto> searchMyEnrollments(Long studentId, String keyword, String level,
                                                 String status, String sort, int page, int size) {
        String lv = CourseLevel.normalize(level);
        String st = status == null ? "" : status.trim().toUpperCase();

        List<MyCourseDto> all = new ArrayList<>();
        for (Enrollment enrollment : enrollmentRepository.findWithCourseByStudentId(studentId)) {
            MyCourseDto dto = MyCourseDto.from(enrollment);
            if (!lv.isEmpty() && !lv.equalsIgnoreCase(dto.jlptLevel())) {
                continue;
            }
            if (!st.isEmpty() && !st.equalsIgnoreCase(dto.courseStatus())) {
                continue;
            }
            if (!ListPaging.containsKeyword(dto.courseName(), keyword)) {
                continue;
            }
            all.add(dto);
        }
        ListPaging.sortBy(all, sort, MyCourseDto::courseName, MyCourseDto::enrolledAt);
        return ListPaging.paginate(all, page, size);
    }
}
