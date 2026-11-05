package com.japaneselms.repository;

import com.japaneselms.entity.Course;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {
    List<Course> findByInstructor_UserId(Long instructorId);
    Page<Course> findByInstructor_UserId(Long instructorId, Pageable pageable);
}
