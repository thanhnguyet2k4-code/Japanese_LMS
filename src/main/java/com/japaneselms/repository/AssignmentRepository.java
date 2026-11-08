
package com.japaneselms.repository;

import com.japaneselms.entity.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository quan ly du lieu Assignment.
 */
public interface AssignmentRepository extends JpaRepository<Assignment, Long> {

    // MF-03: Kiem tra bai tap co thuoc bai hoc hay khong.
    boolean existsByLesson_LessonId(Long lessonId);

    // MF-02: Kiem tra khoa hoc da co bai tap hay chua.
    boolean existsByCourse_CourseId(Long courseId);
}
