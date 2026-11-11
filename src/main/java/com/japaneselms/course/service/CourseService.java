package com.japaneselms.course.service;

import com.japaneselms.course.dto.CourseDetailDto;
import com.japaneselms.course.dto.CourseLevel;
import com.japaneselms.course.dto.CourseSummaryDto;
import com.japaneselms.course.exception.CourseNotFoundException;
import com.japaneselms.entity.Course;
import com.japaneselms.repository.CourseRepository;
import com.japaneselms.repository.LessonRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Set;

/**
 * UC06 + UC07 – Duyệt danh sách và xem chi tiết khóa học (công khai, chỉ đọc).
 * Mọi thao tác map Entity -> DTO diễn ra TRONG transaction vì spring.jpa.open-in-view=false.
 */
@Service
public class CourseService {

    /**
     * Chỉ cho phép sắp xếp theo các thuộc tính này (BR whitelist).
     */
    private static final Set<String> SORTABLE_PROPERTIES = Set.of("courseName", "createdAt");
    private static final Sort DEFAULT_SORT = Sort.by(Sort.Direction.DESC, "createdAt");

    private final CourseRepository courseRepository;
    private final LessonRepository lessonRepository;

    public CourseService(CourseRepository courseRepository, LessonRepository lessonRepository) {
        this.courseRepository = courseRepository;
        this.lessonRepository = lessonRepository;
    }

    /**
     * UC06 – Tìm khóa học ACTIVE theo từ khóa + cấp độ, có phân trang và sắp xếp phía server.
     */
    @Transactional(readOnly = true)
    public Page<CourseSummaryDto> searchCourses(String keyword, String level, Pageable pageable) {
        Pageable safePageable = PageableUtils.sanitize(pageable, SORTABLE_PROPERTIES, DEFAULT_SORT);

        return courseRepository
                .searchActiveCourses(toLikePattern(keyword), CourseLevel.normalize(level), safePageable)
                .map(CourseSummaryDto::from);
    }

    /**
     * UC07 – Chi tiết khóa học ACTIVE. Khóa học INACTIVE hoặc không tồn tại -> CourseNotFoundException (404).
     */
    @Transactional(readOnly = true)
    public CourseDetailDto getActiveCourseDetail(Long courseId) {
        Course course = courseRepository
                .findOpenForStudents(courseId)
                .orElseThrow(() -> new CourseNotFoundException(courseId));

        long lessonCount = lessonRepository
                .countByCourse_CourseIdAndLessonStatus(courseId, CourseStatus.ACTIVE);

        return CourseDetailDto.from(course, lessonCount);
    }

    /**
     * Chuyển từ khóa người dùng nhập thành mẫu LIKE an toàn: "%từ khóa%" (chữ thường).
     * Escape các ký tự đặc biệt của LIKE trên SQL Server (% _ [) bằng ký tự '!' để
     * người dùng gõ "100%" hay "N5_" vẫn tìm đúng chứ không bị hiểu là wildcard.
     */
    private String toLikePattern(String keyword) {
        String value = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);

        value = value.replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_")
                .replace("[", "![");

        return "%" + value + "%";
    }
}
