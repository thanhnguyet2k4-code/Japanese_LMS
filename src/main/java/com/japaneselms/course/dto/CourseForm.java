package com.japaneselms.course.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

public class CourseForm {

    @NotBlank(message = "Vui lòng nhập tên khóa học.")
    @Size(max = 200, message = "Tên khóa học không được vượt quá 200 ký tự.")
    private String courseName;

    @NotBlank(message = "Vui lòng chọn cấp độ JLPT.")
    @Pattern(regexp = "^N[1-5]$", message = "Cấp độ JLPT không hợp lệ.")
    private String jlptLevel = "N5";

    @NotBlank(message = "Vui lòng chọn trạng thái khóa học.")
    @Pattern(regexp = "^(ACTIVE|INACTIVE)$", message = "Trạng thái khóa học không hợp lệ.")
    private String courseStatus = "ACTIVE";

    private MultipartFile coverImage;

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public String getJlptLevel() {
        return jlptLevel;
    }

    public void setJlptLevel(String jlptLevel) {
        this.jlptLevel = jlptLevel;
    }

    public String getCourseStatus() {
        return courseStatus;
    }

    public void setCourseStatus(String courseStatus) {
        this.courseStatus = courseStatus;
    }

    public MultipartFile getCoverImage() {
        return coverImage;
    }

    public void setCoverImage(MultipartFile coverImage) {
        this.coverImage = coverImage;
    }
}
