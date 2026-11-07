package com.japaneselms.repository;

import com.japaneselms.entity.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    @org.springframework.data.jpa.repository.Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from Enrollment e where e.course.courseId = :courseId")
    void deleteAllByCourseId(@Param("courseId") Long courseId);

    boolean existsByStudent_UserIdAndCourse_CourseId(Long studentId, Long courseId);

    @Query("""
            select e from Enrollment e
            join fetch e.course c
            join fetch c.instructor
            where e.student.userId = :studentId
            order by e.enrolledAt desc
            """)
    List<Enrollment> findWithCourseByStudentId(@Param("studentId") Long studentId);
}
