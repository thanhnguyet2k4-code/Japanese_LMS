package com.japaneselms.assignment.controller;

import com.japaneselms.common.ListPaging;
import com.japaneselms.entity.User;
import com.japaneselms.learning.service.LessonService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Trang chọn khóa học cho mục "Chấm bài" của giảng viên.
 * Chọn khóa học -> vào danh sách bài tập (S14) -> xem bài nộp (S18) -> chấm điểm (S20).
 * Không thay đổi các route chấm bài hiện có (/instructor/grading...).
 */
@Controller
public class AssignmentCourseSelectController {

    private final LessonService lessonService;

    public AssignmentCourseSelectController(LessonService lessonService) {
        this.lessonService = lessonService;
    }

    @GetMapping("/instructor/grading-courses")
    public String selectCourse(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String page,
            Model model) {
        ListPaging.populateInstructorCourses(model, lessonService.listInstructorCourses(currentUser),
                keyword, level, status, sort, page);
        model.addAttribute("currentTab", "grading");
        return "instructor/grading-courses";
    }
}
