package com.japaneselms.course.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CourseFormDto {

    private Long courseId;

    @NotBlank(message = "Tên khóa học không được để trống")
    @Size(min = 5, max = 200, message = "Tên khóa học phải từ 5 đến 200 ký tự")
    private String courseName;

    @NotBlank(message = "Vui lòng chọn cấp độ JLPT")
    @Pattern(regexp = "^(N5|N4|N3)$", message = "Cấp độ JLPT chỉ chấp nhận N5, N4 hoặc N3")
    private String jlptLevel;

    @NotBlank(message = "Mô tả khóa học không được để trống")
    @Size(max = 2000, message = "Mô tả khóa học không vượt quá 2000 ký tự")
    private String description;
}