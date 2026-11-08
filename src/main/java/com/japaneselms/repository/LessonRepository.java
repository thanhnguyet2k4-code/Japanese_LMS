package com.japaneselms.repository;

import com.japaneselms.entity.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository của bảng lessons.
 * File gốc của Leader là class rỗng -> đổi thành interface.
 * (Member 3 cũng sẽ thêm hàm vào file này: khi merge chỉ cần giữ cả hai khối code.)
 */
public interface LessonRepository extends JpaRepository<Lesson, Long> {

    // ===================== MF-02 (Member 2) – BẮT ĐẦU =====================

    /**
     * UC07 – Đếm số bài học (ACTIVE) của khóa học để hiển thị ở S07.
     */
    long countByCourse_CourseIdAndLessonStatus(Long courseId, String lessonStatus);

    /**
     * UC08 – Khóa học đã có bài học (bất kể trạng thái) chưa? (quyết định Soft Delete)
     */
    boolean existsByCourse_CourseId(Long courseId);

    // ====================== MF-02 (Member 2) – KẾT THÚC ======================
}
