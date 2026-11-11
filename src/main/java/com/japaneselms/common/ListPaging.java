package com.japaneselms.common;

import com.japaneselms.course.dto.CourseLevel;
import com.japaneselms.entity.Course;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.ui.Model;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

/**
 * Tiện ích dùng chung cho các trang danh sách (thẻ khóa học...):
 * tìm kiếm theo từ khóa (không phân biệt hoa thường và dấu tiếng Việt), lọc cấp độ,
 * sắp xếp và phân trang trên danh sách đã nạp sẵn.
 * Chỉ dùng cho các trang vốn đã trả về toàn bộ danh sách (số lượng nhỏ theo từng người dùng);
 * KHÔNG thay đổi bất kỳ service / repository nào hiện có.
 */
public final class ListPaging {

    /** 9 thẻ / trang = lưới 3 x 3. */
    public static final int PAGE_SIZE = 9;

    private ListPaging() {
    }

    /** Đọc số trang (bắt đầu từ 0) từ request, giá trị lạ -> 0. */
    public static int parsePage(String raw) {
        if (raw == null || raw.isBlank()) {
            return 0;
        }
        try {
            return Math.max(Integer.parseInt(raw.trim()), 0);
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    /** Cắt danh sách thành một trang; số trang vượt quá thì lùi về trang cuối. */
    public static <T> Page<T> paginate(List<T> all, int page, int size) {
        int total = all.size();
        int totalPages = Math.max((int) Math.ceil(total / (double) size), 1);
        int safePage = Math.min(Math.max(page, 0), totalPages - 1);
        int from = Math.min(safePage * size, total);
        int to = Math.min(from + size, total);
        return new PageImpl<>(new ArrayList<>(all.subList(from, to)),
                PageRequest.of(safePage, size), total);
    }

    /** Chuẩn hóa chuỗi để so sánh: chữ thường, bỏ dấu tiếng Việt. */
    public static String fold(String value) {
        if (value == null) {
            return "";
        }
        String s = Normalizer.normalize(value.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        return s.replace('đ', 'd');
    }

    public static boolean containsKeyword(String text, String keyword) {
        String k = fold(keyword);
        return k.isEmpty() || fold(text).contains(k);
    }

    /** Chỉ chấp nhận các giá trị sort nằm trong danh sách cho phép. */
    public static String normalizeSort(String sort, String defaultSort, String... allowed) {
        if (sort != null) {
            for (String a : allowed) {
                if (a.equals(sort.trim())) {
                    return a;
                }
            }
        }
        return defaultSort;
    }

    /**
     * Lọc + sắp xếp danh sách Course theo từ khóa, cấp độ và sort
     * (createdAt,desc | createdAt,asc | courseName,asc | courseName,desc).
     */
    public static List<Course> filterCourses(List<Course> source, String keyword, String level, String sort) {
        String lv = CourseLevel.normalize(level);
        List<Course> result = new ArrayList<>();
        for (Course c : source) {
            if (!lv.isEmpty() && !lv.equalsIgnoreCase(c.getJlptLevel())) {
                continue;
            }
            if (!containsKeyword(c.getCourseName(), keyword)) {
                continue;
            }
            result.add(c);
        }
        // sort rỗng = giữ nguyên thứ tự mặc định của nguồn dữ liệu
        if (sort != null && !sort.isBlank()) {
            sortBy(result, sort, Course::getCourseName, Course::getCreatedAt);
        }
        return result;
    }

    /**
     * Dùng cho các trang thẻ khóa học của giảng viên: lọc theo từ khóa / cấp độ / trạng thái (ACTIVE, INACTIVE),
     * sắp xếp, phân trang rồi đưa kết quả vào Model. Model "courses" vẫn là List<Course> của trang hiện tại
     * nên các vòng lặp th:each trong template cũ không phải đổi.
     */
    public static void populateInstructorCourses(Model model, List<Course> all, String keyword, String level,
                                                 String status, String sort, String page) {
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        String normalizedLevel = CourseLevel.normalize(level);
        String normalizedStatus = "ACTIVE".equalsIgnoreCase(status) ? "ACTIVE"
                : "INACTIVE".equalsIgnoreCase(status) ? "INACTIVE" : "";
        String normalizedSort = normalizeSort(sort, "",
                "createdAt,desc", "createdAt,asc", "courseName,asc", "courseName,desc");

        List<Course> filtered = filterCourses(all, normalizedKeyword, normalizedLevel, normalizedSort);
        if (!normalizedStatus.isEmpty()) {
            filtered = filtered.stream()
                    .filter(c -> normalizedStatus.equalsIgnoreCase(c.getCourseStatus()))
                    .toList();
        }
        Page<Course> pageData = paginate(filtered, parsePage(page), PAGE_SIZE);

        model.addAttribute("courses", pageData.getContent());
        model.addAttribute("totalAll", all.size());
        model.addAttribute("currentPage", pageData.getNumber());
        model.addAttribute("totalPages", pageData.getTotalPages());
        model.addAttribute("totalElements", pageData.getTotalElements());
        model.addAttribute("pageSize", PAGE_SIZE);
        model.addAttribute("keyword", normalizedKeyword);
        model.addAttribute("level", normalizedLevel);
        model.addAttribute("status", normalizedStatus);
        model.addAttribute("sort", normalizedSort);
        model.addAttribute("levels", CourseLevel.values());
    }

    public static <T> void sortBy(List<T> list, String sort,
                                  Function<T, String> nameFn,
                                  Function<T, LocalDateTime> dateFn) {
        String s = sort == null ? "" : sort;
        Comparator<T> byName = Comparator.comparing(t -> fold(nameFn.apply(t)));
        Comparator<T> byDate = Comparator.comparing(dateFn,
                Comparator.nullsFirst(Comparator.<LocalDateTime>naturalOrder()));
        switch (s) {
            case "courseName,asc" -> list.sort(byName);
            case "courseName,desc" -> list.sort(byName.reversed());
            case "createdAt,asc", "enrolledAt,asc" -> list.sort(byDate);
            default -> list.sort(byDate.reversed());
        }
    }
}
