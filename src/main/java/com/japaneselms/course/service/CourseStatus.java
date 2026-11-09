package com.japaneselms.course.service;

/**
 * Hằng số trạng thái dùng chung trong module Course (khớp CHECK constraint trong DB).
 */
public final class CourseStatus {

    public static final String ACTIVE = "ACTIVE";
    public static final String INACTIVE = "INACTIVE";

    private CourseStatus() {
    }
}
