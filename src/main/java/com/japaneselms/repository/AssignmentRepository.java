package com.japaneselms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.japaneselms.entity.Assignment;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {
    boolean existsByCourse_CourseId(Long courseId);
}
