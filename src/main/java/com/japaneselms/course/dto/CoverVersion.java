package com.japaneselms.course.dto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Tham so v= gan vao URL anh bia de trinh duyet tai lai anh khi Instructor doi anh.
 */
final class CoverVersion {

    private static final DateTimeFormatter FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private CoverVersion() {
    }

    static String of(LocalDateTime updatedAt) {
        return updatedAt == null ? "0" : updatedAt.format(FORMAT);
    }
}
