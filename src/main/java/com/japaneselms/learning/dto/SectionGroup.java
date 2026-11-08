package com.japaneselms.learning.dto;

import com.japaneselms.entity.Lesson;

import java.util.List;

public class SectionGroup {

    private Long sectionId;
    private String title;
    private Integer sectionOrder;
    private List<Lesson> lessons;

    public SectionGroup(Long sectionId, String title, Integer sectionOrder, List<Lesson> lessons) {
        this.sectionId = sectionId;
        this.title = title;
        this.sectionOrder = sectionOrder;
        this.lessons = lessons;
    }

    public Long getSectionId() {
        return sectionId;
    }

    public String getTitle() {
        return title;
    }

    public Integer getSectionOrder() {
        return sectionOrder;
    }

    public List<Lesson> getLessons() {
        return lessons;
    }

    public int getLessonCount() {
        return lessons == null ? 0 : lessons.size();
    }
}
