package com.japaneselms.learning.controller;

import com.japaneselms.common.exception.ResourceNotFoundException;
import com.japaneselms.entity.Course;
import com.japaneselms.entity.Lesson;
import com.japaneselms.entity.User;
import com.japaneselms.learning.service.LearningMaterialService;
import com.japaneselms.learning.service.LessonService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class StudentLessonController {

    private final LessonService lessonService;
    private final LearningMaterialService materialService;

    public StudentLessonController(
            LessonService lessonService,
            LearningMaterialService materialService) {
        this.lessonService = lessonService;
        this.materialService = materialService;
    }

    // Trang danh sach khoa hoc da dang ky cua hoc vien
    @GetMapping("/student")
    public String studentHome(
            @AuthenticationPrincipal User currentUser,
            Model model) {
        model.addAttribute("courses", lessonService.listStudentCourses(currentUser));
        model.addAttribute("currentTab", "courses");
        return "student/courses";
    }

    // S10: Xem danh sach bai hoc trong khoa hoc (Student)
    @GetMapping("/student/courses/{courseId}/lessons")
    public String listLessons(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long courseId,
            Model model) {
        Course course = lessonService.getViewableCourse(currentUser, courseId);
        lessonService.fillCourseProgress(currentUser, course);

        model.addAttribute("course", course);
        model.addAttribute("groups", lessonService.groupLessonsForStudent(currentUser, courseId));
        model.addAttribute("currentTab", "lessons");
        return "student/lessons/list";
    }

    // S13: Hoc bai va truy cap tai lieu duoc phep
    @GetMapping("/student/courses/{courseId}/lessons/{lessonId}")
    public String viewLesson(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long courseId,
            @PathVariable Long lessonId,
            Model model) {
        Lesson lesson = lessonService.getViewableLesson(currentUser, lessonId);
        if (!lesson.getCourse().getCourseId().equals(courseId)) {
            throw new ResourceNotFoundException("Không tìm thấy bài học trong khóa học này.");
        }

        Course course = lesson.getCourse();
        lessonService.fillCourseProgress(currentUser, course);
        lesson.setCompleted(lessonService.isLessonCompleted(currentUser, lessonId));

        // Tim bai hoc truoc va bai hoc tiep theo
        List<Lesson> activeLessons = lessonService.listActiveLessonsForStudent(currentUser, courseId);
        Long previousLessonId = null;
        Long nextLessonId = null;
        for (int i = 0; i < activeLessons.size(); i++) {
            if (activeLessons.get(i).getLessonId().equals(lessonId)) {
                if (i > 0) {
                    previousLessonId = activeLessons.get(i - 1).getLessonId();
                }
                if (i < activeLessons.size() - 1) {
                    nextLessonId = activeLessons.get(i + 1).getLessonId();
                }
                break;
            }
        }

        model.addAttribute("course", course);
        model.addAttribute("lesson", lesson);
        model.addAttribute("materials", materialService.listMaterialsByLesson(lessonId));
        model.addAttribute("previousLessonId", previousLessonId);
        model.addAttribute("nextLessonId", nextLessonId);
        model.addAttribute("currentTab", "lessons");
        return "student/lessons/view";
    }

    // Danh dau bai hoc da hoan thanh
    @PostMapping("/student/courses/{courseId}/lessons/{lessonId}/complete")
    public String markCompleted(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long courseId,
            @PathVariable Long lessonId,
            RedirectAttributes redirectAttributes) {
        lessonService.markCompleted(currentUser, lessonId);
        redirectAttributes.addFlashAttribute("successMessage", "Đã đánh dấu bài học hoàn thành.");
        return "redirect:/student/courses/" + courseId + "/lessons/" + lessonId;
    }

    // Bo danh dau hoan thanh bai hoc
    @PostMapping("/student/courses/{courseId}/lessons/{lessonId}/uncomplete")
    public String unmarkCompleted(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long courseId,
            @PathVariable Long lessonId,
            RedirectAttributes redirectAttributes) {
        lessonService.unmarkCompleted(currentUser, lessonId);
        redirectAttributes.addFlashAttribute("successMessage", "Đã bỏ đánh dấu hoàn thành.");
        return "redirect:/student/courses/" + courseId + "/lessons/" + lessonId;
    }
}
