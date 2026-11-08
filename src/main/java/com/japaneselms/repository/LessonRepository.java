
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

    // MF-03: Kiem tra Section da co Lesson hay chua.
    boolean existsBySection_SectionId(Long sectionId);

    // MF-03: Xoa tat ca Lesson cua mot Course.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from Lesson l where l.course.courseId = :courseId")
    void deleteAllByCourseId(@Param("courseId") Long courseId);

    // MF-03: Lay danh sach Lesson kem Course va Section.
    @Query("""
            select l from Lesson l
            join fetch l.course c
            left join fetch l.section
            where c.courseId = :courseId
            order by l.lessonOrder asc
            """)
    List<Lesson> findByCourseIdOrderByLessonOrderAsc(
            @Param("courseId") Long courseId
    );

    // MF-04: Lay danh sach Lesson theo Course, sap xep theo thu tu.
    List<Lesson> findByCourse_CourseIdOrderByLessonOrderAsc(Long courseId);

    // MF-03: Lay Lesson kem Course, Section va Instructor.
    @Query("""
            select l from Lesson l
            join fetch l.course c
            left join fetch l.section
            join fetch c.instructor
            where l.lessonId = :lessonId
            """)
    Optional<Lesson> findWithCourseById(
            @Param("lessonId") Long lessonId
    );

    // MF-03: Kiem tra trung thu tu Lesson trong Course.
    boolean existsByCourse_CourseIdAndLessonOrder(
            Long courseId,
            Integer lessonOrder
    );

    // MF-03: Kiem tra trung thu tu Lesson khi cap nhat.
    boolean existsByCourse_CourseIdAndLessonOrderAndLessonIdNot(
            Long courseId,
            Integer lessonOrder,
            Long lessonId
    );

    // MF-03: Dem Lesson theo trang thai, khong phan biet chu hoa/thuong.
    long countByCourse_CourseIdAndLessonStatusIgnoreCase(
            Long courseId,
            String lessonStatus
    );

    // MF-02: Dem Lesson theo trang thai.
    long countByCourse_CourseIdAndLessonStatus(
            Long courseId,
            String lessonStatus
    );

    // MF-02: Kiem tra Course da co Lesson hay chua.
    boolean existsByCourse_CourseId(Long courseId);
}
