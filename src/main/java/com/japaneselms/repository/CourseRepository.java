package com.japaneselms.repository;

import com.japaneselms.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Long> {

    List<Course> findByInstructor_UserIdOrderByCreatedAtDesc(Long instructorId);



    @Query("""
            select c from Course c
            join fetch c.instructor
            where c.courseId = :courseId
            """)
    Optional<Course> findWithInstructorById(@Param("courseId") Long courseId);
}
