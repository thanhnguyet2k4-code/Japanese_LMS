package com.japaneselms.repository;

import com.japaneselms.entity.Course;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * Repository của bảng courses.
 * <p>
 * LƯU Ý KHI MERGE: file gốc của Leader là "public class CourseRepository {}" (class rỗng).
 * Ở đây đổi thành interface để Spring Data JPA sinh implementation.
 * Toàn bộ phần của Member 2 nằm trong khối MF-02 bên dưới.
 */
public interface CourseRepository extends JpaRepository<Course, Long> {

    // ===================== MF-02 (Member 2) – BẮT ĐẦU =====================

    /**
     * UC06 – Tìm khóa học ACTIVE theo tên (không phân biệt hoa/thường, khớp một phần)
     * và lọc theo cấp độ JLPT.
     * <p>
     * - pattern: đã được Service chuẩn hóa thành dạng "%từ khóa%" (chữ thường, đã escape % _ [ bằng '!').
     * - level  : "" = không lọc; "N5"/"N4"/"N3" = lọc theo cấp độ. Dùng chuỗi rỗng thay vì null
     * để tránh lỗi suy luận kiểu tham số null trên SQL Server.
     * - @EntityGraph nạp luôn instructor (JOIN) để tránh N+1 khi hiển thị tên giảng viên.
     * - Sắp xếp + phân trang do Pageable quyết định (đã được Service lọc whitelist).
     */
    @EntityGraph(attributePaths = "instructor")
    @Query(value = """
            SELECT c FROM Course c
            WHERE c.courseStatus = 'ACTIVE'
              AND LOWER(c.courseName) LIKE :pattern ESCAPE '!'
              AND (:level = '' OR c.jlptLevel = :level)
            """,
            countQuery = """
                    SELECT COUNT(c) FROM Course c
                    WHERE c.courseStatus = 'ACTIVE'
                      AND LOWER(c.courseName) LIKE :pattern ESCAPE '!'
                      AND (:level = '' OR c.jlptLevel = :level)
                    """)
    Page<Course> searchActiveCourses(@Param("pattern") String pattern,
                                     @Param("level") String level,
                                     Pageable pageable);

    /**
     * UC07 – Lấy chi tiết khóa học theo id + trạng thái (dùng với "ACTIVE"), kèm instructor.
     */
    @EntityGraph(attributePaths = "instructor")
    Optional<Course> findByCourseIdAndCourseStatus(Long courseId, String courseStatus);

    /**
     * UC08 – Danh sách khóa học của một Instructor (gồm cả ACTIVE và INACTIVE).
     */
    @EntityGraph(attributePaths = "instructor")
    Page<Course> findByInstructor_UserId(Long instructorId, Pageable pageable);

    /**
     * UC08 – Kiểm tra trùng tên khóa học trong cùng Instructor (khi tạo mới).
     */
    boolean existsByInstructor_UserIdAndCourseNameIgnoreCase(Long instructorId, String courseName);

    /**
     * UC08 – Kiểm tra trùng tên khi sửa (loại trừ chính khóa học đang sửa).
     */
    boolean existsByInstructor_UserIdAndCourseNameIgnoreCaseAndCourseIdNot(
            Long instructorId, String courseName, Long courseId);

    // ====================== MF-02 (Member 2) – KẾT THÚC ======================
}
