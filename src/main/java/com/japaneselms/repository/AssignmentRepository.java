package com.japaneselms.repository;

import com.japaneselms.entity.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository của bảng assignments.
 * File gốc của Leader là class rỗng -> đổi thành interface.
 */
public interface AssignmentRepository extends JpaRepository<Assignment, Long> {

    // ===================== MF-02 (Member 2) – BẮT ĐẦU =====================

    /**
     * UC08 – Khóa học đã có bài tập chưa? (quyết định Soft Delete)
     */
    boolean existsByCourse_CourseId(Long courseId);

    // ====================== MF-02 (Member 2) – KẾT THÚC ======================
}
