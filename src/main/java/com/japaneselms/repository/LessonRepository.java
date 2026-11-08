
package com.japaneselms.repository;

import com.japaneselms.entity.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Repository quan ly du lieu Lesson.
 */
public interface LessonRepository extends JpaRepository<Lesson, Long> {

    // ==================== MF-03: LESSON MANAGEMENT ====================

    boolean existsBySection_SectionId(Long sectionId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from Lesson l where l.course.courseId = :courseId")
    void deleteAllByCourseId(@Param("courseId") Long courseId);

    @Query("""
            select l from Lesson l
            join fetch l.course c
            left join fetch l.section
            where c.courseId = :courseId
            order by l.lessonOrder asc
            """)
    List<Lesson> findByCourseIdOrderByLessonOrderAsc(
            @Param("courseId") Long courseId);

    @Query("""
            select l from Lesson l
            join fetch l.course c
            left join fetch l.section
            join fetch c.instructor
            where l.lessonId = :lessonId
            """)
    Optional<Lesson> findWithCourseById(
            @Param("lessonId") Long lessonId);

    boolean existsByCourse_CourseIdAndLessonOrder(
            Long courseId, Integer lessonOrder);

    boolean existsByCourse_CourseIdAndLessonOrderAndLessonIdNot(
            Long courseId,
            Integer lessonOrder,
            Long lessonId);

    long countByCourse_CourseIdAndLessonStatusIgnoreCase(
            Long courseId, String lessonStatus);


    // ==================== MF-02: COURSE MANAGEMENT ====================

    /**
     * UC07 - Dem so bai hoc theo trang thai cua khoa hoc.
     */
    long countByCourse_CourseIdAndLessonStatus(
            Long courseId, String lessonStatus);

    /**
     * UC08 - Kiem tra khoa hoc da co bai hoc hay chua.
     */
    boolean existsByCourse_CourseId(Long courseId);
}
