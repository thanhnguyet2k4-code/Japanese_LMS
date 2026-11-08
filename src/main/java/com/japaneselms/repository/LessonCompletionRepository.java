package com.japaneselms.repository;

import com.japaneselms.entity.LessonCompletion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Set;

public interface LessonCompletionRepository extends JpaRepository<LessonCompletion, Long> {

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from LessonCompletion c where c.lesson.lessonId = :lessonId")
    void deleteAllByLessonId(@Param("lessonId") Long lessonId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from LessonCompletion c where c.lesson.lessonId in "
            + "(select l.lessonId from Lesson l where l.course.courseId = :courseId)")
    void deleteAllByCourseId(@Param("courseId") Long courseId);

    boolean existsByStudent_UserIdAndLesson_LessonId(Long studentId, Long lessonId);

    @Modifying
    void deleteByStudent_UserIdAndLesson_LessonId(Long studentId, Long lessonId);

    @Query("""
            select c.lesson.lessonId
            from LessonCompletion c
            where c.student.userId = :studentId
              and c.lesson.course.courseId = :courseId
            """)
    Set<Long> findCompletedLessonIds(
            @Param("studentId") Long studentId,
            @Param("courseId") Long courseId
    );

    @Query("""
            select count(c)
            from LessonCompletion c
            where c.student.userId = :studentId
              and c.lesson.course.courseId = :courseId
              and upper(c.lesson.lessonStatus) = 'ACTIVE'
            """)
    long countCompletedActiveLessons(
            @Param("studentId") Long studentId,
            @Param("courseId") Long courseId
    );
}
