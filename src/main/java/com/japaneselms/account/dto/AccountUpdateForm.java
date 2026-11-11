package com.japaneselms.account.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class AccountUpdateForm {

    private Long userId;

    @NotBlank(message = "Vui lòng nhập họ và tên.")
    @Size(max = 100, message = "Họ và tên không được vượt quá 100 ký tự.")
    private String fullName;

    @NotBlank(message = "Vui lòng nhập địa chỉ email.")
    @Pattern(
            regexp = "^(\\s*|[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,})$",
            message = "Email không đúng định dạng."
    )
    @Size(max = 150, message = "Email không được vượt quá 150 ký tự.")
    private String email;

    @Size(max = 20, message = "Số điện thoại không được vượt quá 20 ký tự.")
    @Pattern(
            regexp = "^$|^\\+?(?=.*\\d)[0-9()\\s-]{1,20}$",
            message = "Số điện thoại không đúng định dạng."
    )
    private String phone;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}