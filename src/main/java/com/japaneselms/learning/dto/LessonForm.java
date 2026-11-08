package com.japaneselms.learning.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class LessonForm {

    @NotBlank(message = "Vui lòng nhập tiêu đề bài học.")
    @Size(max = 200, message = "Tiêu đề bài học không được vượt quá 200 ký tự.")
    private String lessonTitle;

    private String lessonDescription;

    @NotNull(message = "Vui lòng nhập thứ tự bài học.")
    @Min(value = 1, message = "Thứ tự bài học phải lớn hơn 0.")
    private Integer lessonOrder;

    @NotBlank(message = "Vui lòng chọn trạng thái bài học.")
    private String lessonStatus = "ACTIVE";

    private Long sectionId;

    public String getLessonTitle() {
        return lessonTitle;
    }

    public void setLessonTitle(String lessonTitle) {
        this.lessonTitle = lessonTitle;
    }

    public String getLessonDescription() {
        return lessonDescription;
    }

    public void setLessonDescription(String lessonDescription) {
        this.lessonDescription = lessonDescription;
    }

    public Integer getLessonOrder() {
        return lessonOrder;
    }

    public void setLessonOrder(Integer lessonOrder) {
        this.lessonOrder = lessonOrder;
    }

    public String getLessonStatus() {
        return lessonStatus;
    }

    public void setLessonStatus(String lessonStatus) {
        this.lessonStatus = lessonStatus;
    }

    public Long getSectionId() {
        return sectionId;
    }

    public void setSectionId(Long sectionId) {
        this.sectionId = sectionId;
    }
}
