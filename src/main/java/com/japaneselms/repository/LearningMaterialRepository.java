package com.japaneselms.repository;

import com.japaneselms.entity.LearningMaterial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LearningMaterialRepository extends JpaRepository<LearningMaterial, Long> {

    @Query("select m.storagePath from LearningMaterial m "
            + "where m.lesson.lessonId = :lessonId and m.storagePath is not null")
    List<String> findStoragePathsByLessonId(@Param("lessonId") Long lessonId);

    @Query("select m.storagePath from LearningMaterial m "
            + "where m.lesson.course.courseId = :courseId and m.storagePath is not null")
    List<String> findStoragePathsByCourseId(@Param("courseId") Long courseId);

    @org.springframework.data.jpa.repository.Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from LearningMaterial m where m.lesson.lessonId = :lessonId")
    void deleteAllByLessonId(@Param("lessonId") Long lessonId);

    @org.springframework.data.jpa.repository.Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from LearningMaterial m where m.lesson.lessonId in "
            + "(select l.lessonId from Lesson l where l.course.courseId = :courseId)")
    void deleteAllByCourseId(@Param("courseId") Long courseId);

    List<LearningMaterial> findByLesson_LessonIdOrderByCreatedAtAsc(Long lessonId);

    long countByLesson_LessonId(Long lessonId);

    @Query("""
            select m from LearningMaterial m
            join fetch m.lesson l
            join fetch l.course c
            join fetch c.instructor
            where m.materialId = :materialId
            """)
    Optional<LearningMaterial> findWithLessonAndCourseById(@Param("materialId") Long materialId);
}
