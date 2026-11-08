
package com.japaneselms.course.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

/**
 * DTO cua form tao/sua khoa hoc.
 * Khong chua courseId hoac instructor de tranh mass assignment.
 */
public class CourseForm {

    @NotBlank(message = "Vui lòng nhập tên khóa học.")
    @Size(min = 5, max = 150,
            message = "Tên khóa học phải từ 5 đến 150 ký tự.")
    private String courseName;

    @NotBlank(message = "Vui lòng chọn cấp độ JLPT.")
    @Pattern(regexp = "^N[1-5]$",
            message = "Cấp độ JLPT không hợp lệ.")
    private String jlptLevel = "N5";

    @NotBlank(message = "Vui lòng nhập mô tả khóa học.")
    @Size(max = 2000,
            message = "Mô tả không được vượt quá 2000 ký tự.")
    private String description;

    private MultipartFile coverImage;

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName == null ? null : courseName.trim();
    }

    public String getJlptLevel() {
        return jlptLevel;
    }

    public void setJlptLevel(String jlptLevel) {
        this.jlptLevel = jlptLevel;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public MultipartFile getCoverImage() {
        return coverImage;
    }

    public void setCoverImage(MultipartFile coverImage) {
        this.coverImage = coverImage;
    }
}
