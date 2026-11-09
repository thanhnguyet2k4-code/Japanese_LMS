package com.japaneselms.repository;

import com.japaneselms.entity.CourseSection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CourseSectionRepository extends JpaRepository<CourseSection, Long> {

    List<CourseSection> findByCourse_CourseIdOrderBySectionOrderAsc(Long courseId);

    boolean existsByCourse_CourseIdAndSectionOrder(Long courseId, Integer sectionOrder);

    boolean existsByCourse_CourseIdAndSectionOrderAndSectionIdNot(
            Long courseId, Integer sectionOrder, Long sectionId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from CourseSection s where s.course.courseId = :courseId")
    void deleteAllByCourseId(@Param("courseId") Long courseId);
}
