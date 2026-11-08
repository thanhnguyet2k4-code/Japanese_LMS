package com.japaneselms.course.dto;

/**
 * Các cấp độ JLPT được hệ thống hỗ trợ (khớp CHECK CK_courses_level trong DB).
 */
public enum CourseLevel {
    N5, N4, N3;

    /**
     * Chuẩn hóa tham số level từ request: trim + chữ hoa.
     * Trả về "N5"/"N4"/"N3", hoặc "" nếu rỗng/không hợp lệ (nghĩa là "không lọc").
     */
    public static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        String value = raw.trim().toUpperCase();
        for (CourseLevel level : values()) {
            if (level.name().equals(value)) {
                return value;
            }
        }
        return "";
    }
}
