package com.japaneselms.repository;

import com.japaneselms.entity.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
    boolean existsByStudent_UserIdAndCourse_CourseId(Long studentId, Long courseId);
    Optional<Enrollment> findByStudent_UserIdAndCourse_CourseId(Long studentId, Long courseId);
}
