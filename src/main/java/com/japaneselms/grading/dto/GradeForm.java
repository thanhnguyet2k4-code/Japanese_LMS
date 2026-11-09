package com.japaneselms.grading.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public class GradeForm {
    @NotNull(message = "Vui lòng nhập điểm")
    @DecimalMin(value = "0.0", message = "Điểm tối thiểu là 0")
    @DecimalMax(value = "10.0", message = "Điểm tối đa là 10")
    @Digits(integer = 2, fraction = 1, message = "Chỉ nhập tối đa 1 chữ số thập phân")
    private BigDecimal grade;

    @Size(max = 2000, message = "Nhận xét tối đa 2000 ký tự")
    private String feedback;

    public BigDecimal getGrade() { return grade; }
    public void setGrade(BigDecimal grade) { this.grade = grade; }
    public String getFeedback() { return feedback; }
    public void setFeedback(String feedback) { this.feedback = feedback; }
}
