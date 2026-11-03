package com.japaneselms.repository;

import com.japaneselms.entity.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LessonRepository extends JpaRepository<Lesson, Long> {
    long countByCourse_CourseIdAndLessonStatus(Long courseId, String lessonStatus);
    boolean existsByCourse_CourseId(Long courseId);
}