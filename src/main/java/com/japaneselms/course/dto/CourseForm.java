package com.japaneselms.course.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO của form tạo/sửa khóa học (S09). Chỉ có 3 trường người dùng được phép nhập:
 * không có courseId, instructor, courseStatus, createdAt -> chống mass assignment (BR6).
 * Tên trường giữ đúng tên trong Entity Course.
 */
public class CourseForm {

    @NotBlank(message = "Vui lòng nhập tên khóa học.")
    @Size(min = 5, max = 150, message = "Tên khóa học phải từ 5 đến 150 ký tự.")
    private String courseName;

    @NotBlank(message = "Vui lòng chọn cấp độ JLPT.")
    @Pattern(regexp = "N5|N4|N3", message = "Cấp độ chỉ được là N5, N4 hoặc N3.")
    private String jlptLevel;

    @NotBlank(message = "Vui lòng nhập mô tả khóa học.")
    @Size(max = 2000, message = "Mô tả không được vượt quá 2000 ký tự.")
    private String description;

    public String getCourseName() {
        return courseName;
    }

    // Trim ngay khi binding để "  abc  " không vượt qua được @Size(min = 5)
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
}
