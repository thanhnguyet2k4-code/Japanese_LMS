package com.japaneselms.course.exception;

/**
 * Lỗi nghiệp vụ khi đăng ký khóa học (UC09). Message là câu hiển thị thẳng cho người dùng.
 * Reason giúp Controller quyết định redirect về S07 hay S06.
 */
public class EnrollmentException extends RuntimeException {

    public enum Reason {
        ALREADY_ENROLLED,       // A2: đã đăng ký trước đó
        COURSE_INACTIVE,        // E4: khóa học đã ngừng hoạt động
        ACCOUNT_NOT_ELIGIBLE    // tài khoản không phải STUDENT / chưa APPROVED / đã bị khóa
    }

    private final Reason reason;

    public EnrollmentException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }
}
