package com.japaneselms.account.dto;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class AccountCreateForm {

    @NotBlank(message = "Vui lòng nhập họ và tên.")
    @Size(max = 100, message = "Họ và tên không được vượt quá 100 ký tự.")
    private String fullName;

    @NotBlank(message = "Vui lòng nhập địa chỉ email.")
    @Email(message = "Email không đúng định dạng.")
    @Size(max = 150, message = "Email không được vượt quá 150 ký tự.")
    private String email;

    @NotBlank(message = "Vui lòng nhập mật khẩu ban đầu.")
    @Size(min = 8, message = "Mật khẩu phải chứa ít nhất 8 ký tự.")
    private String initialPassword;

    @NotBlank(message = "Vui lòng chọn vai trò.")
    @Pattern(
            regexp = "^(INSTRUCTOR|ADMIN)?$",
            message = "Vai trò không hợp lệ."
    )
    private String role;

    @Size(max = 20, message = "Số điện thoại không được vượt quá 20 ký tự.")
    private String phone;

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

    public String getInitialPassword() {
        return initialPassword;
    }

    public void setInitialPassword(String initialPassword) {
        this.initialPassword = initialPassword;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}

