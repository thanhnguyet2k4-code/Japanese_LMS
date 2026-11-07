package com.japaneselms.repository;

import com.japaneselms.entity.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {

    boolean existsByLesson_LessonId(Long lessonId);

    boolean existsByCourse_CourseId(Long courseId);
}
