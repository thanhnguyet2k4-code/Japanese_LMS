package com.japaneselms.learning.service;

import com.japaneselms.common.exception.ResourceNotFoundException;
import com.japaneselms.entity.*;
import com.japaneselms.learning.dto.LessonForm;
import com.japaneselms.learning.dto.SectionGroup;
import com.japaneselms.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class LessonService {

    private final LessonRepository lessonRepository;
    private final CourseRepository courseRepository;
    private final CourseSectionRepository sectionRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final LearningMaterialRepository materialRepository;
    private final LessonCompletionRepository completionRepository;
    private final AssignmentRepository assignmentRepository;
    private final Path materialsRootDir;

    public LessonService(
            LessonRepository lessonRepository,
            CourseRepository courseRepository,
            CourseSectionRepository sectionRepository,
            EnrollmentRepository enrollmentRepository,
            LearningMaterialRepository materialRepository,
            LessonCompletionRepository completionRepository,
            AssignmentRepository assignmentRepository,
            @Value("${app.upload.materials-dir:uploads/materials}") String materialsDir) {
        this.lessonRepository = lessonRepository;
        this.courseRepository = courseRepository;
        this.sectionRepository = sectionRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.materialRepository = materialRepository;
        this.completionRepository = completionRepository;
        this.assignmentRepository = assignmentRepository;
        this.materialsRootDir = Paths.get(materialsDir).toAbsolutePath().normalize();
    }

    // ==================== KIEM TRA QUYEN TRUY CAP ====================

    public boolean isAdmin(User user) {
        return user != null && "ADMIN".equalsIgnoreCase(user.getRole());
    }

    public boolean isInstructor(User user) {
        return user != null && "INSTRUCTOR".equalsIgnoreCase(user.getRole());
    }

    public boolean isStudent(User user) {
        return user != null && "STUDENT".equalsIgnoreCase(user.getRole());
    }

    public boolean isInstructorOfCourse(User user, Course course) {
        return isInstructor(user)
                && course != null
                && course.getInstructor() != null
                && user.getUserId().equals(course.getInstructor().getUserId());
    }

    public boolean isEnrolledStudent(User user, Course course) {
        if (!isStudent(user) || course == null) {
            return false;
        }
        return enrollmentRepository.existsByStudent_UserIdAndCourse_CourseId(
                user.getUserId(), course.getCourseId());
    }

    @Transactional(readOnly = true)
    public Course getManageableCourse(User currentUser, Long courseId) {
        Course course = courseRepository.findWithInstructorById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khóa học."));

        if (isAdmin(currentUser) || isInstructorOfCourse(currentUser, course)) {
            return course;
        }
        throw new AccessDeniedException("Bạn không có quyền quản lý khóa học này.");
    }

    @Transactional(readOnly = true)
    public Course getViewableCourse(User currentUser, Long courseId) {
        Course course = courseRepository.findWithInstructorById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khóa học."));

        if (isAdmin(currentUser)
                || isInstructorOfCourse(currentUser, course)
                || isEnrolledStudent(currentUser, course)) {
            return course;
        }
        throw new AccessDeniedException("Bạn không có quyền xem khóa học này.");
    }

    @Transactional(readOnly = true)
    public Lesson getManageableLesson(User currentUser, Long lessonId) {
        Lesson lesson = lessonRepository.findWithCourseById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài học."));

        getManageableCourse(currentUser, lesson.getCourse().getCourseId());
        return lesson;
    }

    @Transactional(readOnly = true)
    public Lesson getViewableLesson(User currentUser, Long lessonId) {
        Lesson lesson = lessonRepository.findWithCourseById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài học."));

        Course course = getViewableCourse(currentUser, lesson.getCourse().getCourseId());

        if (isStudent(currentUser)) {
            if (!"ACTIVE".equalsIgnoreCase(course.getCourseStatus())) {
                throw new AccessDeniedException("Khóa học này hiện không khả dụng.");
            }
            if (!"ACTIVE".equalsIgnoreCase(lesson.getLessonStatus())) {
                throw new AccessDeniedException("Bài học này hiện không khả dụng.");
            }
        }
        return lesson;
    }

    // ==================== DANH SACH KHOA HOC ====================

    @Transactional(readOnly = true)
    public List<Course> listInstructorCourses(User currentUser) {
        if (isAdmin(currentUser)) {
            return courseRepository.findAll();
        }
        // Khoa hoc chua co bai hoc nam o muc "Khoa hoc cho", khong hien o day.
        return courseRepository.findByInstructor_UserIdOrderByCreatedAtDesc(currentUser.getUserId())
                .stream()
                .filter(c -> lessonRepository.existsByCourse_CourseId(c.getCourseId()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Course> listStudentCourses(User currentUser) {
        List<Enrollment> enrollments = enrollmentRepository.findWithCourseByStudentId(currentUser.getUserId());
        List<Course> courses = new ArrayList<>();
        for (Enrollment enrollment : enrollments) {
            Course course = enrollment.getCourse();
            fillCourseProgress(currentUser, course);
            courses.add(course);
        }
        return courses;
    }

    // Tinh tien do hoc tap cua hoc vien trong khoa hoc
    @Transactional(readOnly = true)
    public void fillCourseProgress(User currentUser, Course course) {
        int total = (int) lessonRepository.countByCourse_CourseIdAndLessonStatusIgnoreCase(
                course.getCourseId(), "ACTIVE");
        int completed = (int) completionRepository.countCompletedActiveLessons(
                currentUser.getUserId(), course.getCourseId());

        if (completed > total) {
            completed = total;
        }
        int percent = total == 0 ? 0 : (int) Math.round(completed * 100.0 / total);

        course.setTotalLessons(total);
        course.setCompletedLessons(completed);
        course.setProgressPercent(percent);
    }

    // ==================== QUAN LY BAI HOC (S10, S11, S13) ====================

    @Transactional(readOnly = true)
    public List<Lesson> listLessonsForInstructor(User currentUser, Long courseId) {
        getManageableCourse(currentUser, courseId);
        List<Lesson> lessons = lessonRepository.findByCourseIdOrderByLessonOrderAsc(courseId);
        for (Lesson lesson : lessons) {
            lesson.setMaterialCount(materialRepository.countByLesson_LessonId(lesson.getLessonId()));
        }
        return lessons;
    }

    @Transactional(readOnly = true)
    public List<Lesson> listActiveLessonsForStudent(User currentUser, Long courseId) {
        getViewableCourse(currentUser, courseId);
        Set<Long> completedIds = completionRepository.findCompletedLessonIds(
                currentUser.getUserId(), courseId);

        List<Lesson> allLessons = lessonRepository.findByCourseIdOrderByLessonOrderAsc(courseId);
        List<Lesson> activeLessons = new ArrayList<>();

        for (Lesson lesson : allLessons) {
            if ("ACTIVE".equalsIgnoreCase(lesson.getLessonStatus())) {
                lesson.setMaterialCount(materialRepository.countByLesson_LessonId(lesson.getLessonId()));
                lesson.setCompleted(completedIds.contains(lesson.getLessonId()));
                activeLessons.add(lesson);
            }
        }
        return activeLessons;
    }

    @Transactional(readOnly = true)
    public LessonForm getLessonForm(User currentUser, Long lessonId) {
        Lesson lesson = getManageableLesson(currentUser, lessonId);
        LessonForm form = new LessonForm();
        form.setLessonTitle(lesson.getLessonTitle());
        form.setLessonDescription(lesson.getLessonDescription());
        form.setLessonOrder(lesson.getLessonOrder());
        form.setLessonStatus(lesson.getLessonStatus());
        form.setSectionId(lesson.getSection() != null ? lesson.getSection().getSectionId() : null);
        return form;
    }

    @Transactional
    public Lesson createLesson(User currentUser, Long courseId, LessonForm form) {
        Course course = getManageableCourse(currentUser, courseId);

        if (lessonRepository.existsByCourse_CourseIdAndLessonOrder(courseId, form.getLessonOrder())) {
            throw new IllegalArgumentException(
                    "Thứ tự bài học này đã tồn tại trong khóa học. Vui lòng chọn số khác.");
        }

        Lesson lesson = new Lesson();
        lesson.setCourse(course);
        lesson.setSection(findSectionInCourse(courseId, form.getSectionId()));
        applyLessonForm(lesson, form);
        return lessonRepository.save(lesson);
    }

    @Transactional
    public Lesson updateLesson(User currentUser, Long lessonId, LessonForm form) {
        Lesson lesson = getManageableLesson(currentUser, lessonId);
        Long courseId = lesson.getCourse().getCourseId();

        if (lessonRepository.existsByCourse_CourseIdAndLessonOrderAndLessonIdNot(
                courseId, form.getLessonOrder(), lessonId)) {
            throw new IllegalArgumentException(
                    "Thứ tự bài học này đã tồn tại trong khóa học. Vui lòng chọn số khác.");
        }

        lesson.setSection(findSectionInCourse(courseId, form.getSectionId()));
        applyLessonForm(lesson, form);
        return lessonRepository.save(lesson);
    }

    @Transactional
    public void deactivateLesson(User currentUser, Long lessonId) {
        Lesson lesson = getManageableLesson(currentUser, lessonId);
        lesson.setLessonStatus("INACTIVE");
        lessonRepository.save(lesson);
    }

    @Transactional
    public void deleteLesson(User currentUser, Long courseId, Long lessonId) {
        Lesson lesson = getManageableLesson(currentUser, lessonId);
        if (!lesson.getCourse().getCourseId().equals(courseId)) {
            throw new IllegalArgumentException("Bài học không thuộc khóa học này.");
        }
        if (assignmentRepository.existsByLesson_LessonId(lessonId)) {
            throw new IllegalArgumentException(
                    "Không thể xóa bài học vì đã có bài tập gắn với bài học này.");
        }

        List<String> filePaths = materialRepository.findStoragePathsByLessonId(lessonId);
        completionRepository.deleteAllByLessonId(lessonId);
        materialRepository.deleteAllByLessonId(lessonId);
        lessonRepository.deleteById(lessonId);

        // Xoa cac file tai lieu cua bai hoc tren oung
        for (String path : filePaths) {
            deletePhysicalFile(path);
        }
    }

    private void applyLessonForm(Lesson lesson, LessonForm form) {
        String status = form.getLessonStatus() == null ? "" : form.getLessonStatus().trim().toUpperCase();
        if (!"ACTIVE".equals(status) && !"INACTIVE".equals(status)) {
            throw new IllegalArgumentException("Trạng thái bài học không hợp lệ.");
        }

        lesson.setLessonTitle(form.getLessonTitle().trim());
        String desc = form.getLessonDescription();
        lesson.setLessonDescription(desc == null || desc.isBlank() ? null : desc.trim());
        lesson.setLessonOrder(form.getLessonOrder());
        lesson.setLessonStatus(status);
    }

    private void deletePhysicalFile(String storagePath) {
        if (storagePath == null || storagePath.isBlank()) {
            return;
        }
        try {
            Path filePath = materialsRootDir.resolve(storagePath).normalize();
            Files.deleteIfExists(filePath);
        } catch (Exception ignored) {
        }
    }

    // ==================== TIEN DO HOC BAI CUA HOC VIEN ====================

    @Transactional(readOnly = true)
    public boolean isLessonCompleted(User currentUser, Long lessonId) {
        return completionRepository.existsByStudent_UserIdAndLesson_LessonId(
                currentUser.getUserId(), lessonId);
    }

    @Transactional
    public void markCompleted(User currentUser, Long lessonId) {
        if (!isStudent(currentUser)) {
            throw new AccessDeniedException("Chỉ học viên mới đánh dấu tiến độ học tập.");
        }
        Lesson lesson = getViewableLesson(currentUser, lessonId);
        if (completionRepository.existsByStudent_UserIdAndLesson_LessonId(
                currentUser.getUserId(), lessonId)) {
            return;
        }

        LessonCompletion completion = new LessonCompletion();
        completion.setStudent(currentUser);
        completion.setLesson(lesson);
        completionRepository.save(completion);
    }

    @Transactional
    public void unmarkCompleted(User currentUser, Long lessonId) {
        if (!isStudent(currentUser)) {
            throw new AccessDeniedException("Chỉ học viên mới đánh dấu tiến độ học tập.");
        }
        getViewableLesson(currentUser, lessonId);
        completionRepository.deleteByStudent_UserIdAndLesson_LessonId(
                currentUser.getUserId(), lessonId);
    }

    // ==================== QUAN LY PHAN HOC (SECTION) ====================

    @Transactional(readOnly = true)
    public List<CourseSection> listSections(User currentUser, Long courseId) {
        getManageableCourse(currentUser, courseId);
        return sectionRepository.findByCourse_CourseIdOrderBySectionOrderAsc(courseId);
    }

    @Transactional(readOnly = true)
    public List<SectionGroup> groupLessonsForInstructor(User currentUser, Long courseId) {
        List<Lesson> lessons = listLessonsForInstructor(currentUser, courseId);
        return buildSectionGroups(courseId, lessons, true);
    }

    @Transactional(readOnly = true)
    public List<SectionGroup> groupLessonsForStudent(User currentUser, Long courseId) {
        List<Lesson> lessons = listActiveLessonsForStudent(currentUser, courseId);
        return buildSectionGroups(courseId, lessons, false);
    }

    private List<SectionGroup> buildSectionGroups(
            Long courseId, List<Lesson> lessons, boolean includeEmptySections) {
        List<SectionGroup> groups = new ArrayList<>();
        Set<Long> sectionIds = new HashSet<>();

        List<CourseSection> sections = sectionRepository.findByCourse_CourseIdOrderBySectionOrderAsc(courseId);
        for (CourseSection section : sections) {
            sectionIds.add(section.getSectionId());
            List<Lesson> inSection = new ArrayList<>();
            for (Lesson lesson : lessons) {
                if (lesson.getSection() != null
                        && section.getSectionId().equals(lesson.getSection().getSectionId())) {
                    inSection.add(lesson);
                }
            }
            if (includeEmptySections || !inSection.isEmpty()) {
                groups.add(new SectionGroup(
                        section.getSectionId(),
                        section.getSectionTitle(),
                        section.getSectionOrder(),
                        inSection));
            }
        }

        List<Lesson> ungrouped = new ArrayList<>();
        for (Lesson lesson : lessons) {
            Long secId = lesson.getSection() != null ? lesson.getSection().getSectionId() : null;
            if (secId == null || !sectionIds.contains(secId)) {
                ungrouped.add(lesson);
            }
        }
        if (!ungrouped.isEmpty()) {
            groups.add(new SectionGroup(null, "Bài học chưa phân phần", null, ungrouped));
        }

        return groups;
    }

    @Transactional(readOnly = true)
    public int nextSectionOrder(User currentUser, Long courseId) {
        List<CourseSection> sections = listSections(currentUser, courseId);
        int maxOrder = -1;
        for (CourseSection s : sections) {
            if (s.getSectionOrder() != null && s.getSectionOrder() > maxOrder) {
                maxOrder = s.getSectionOrder();
            }
        }
        return maxOrder + 1;
    }

    @Transactional(readOnly = true)
    public CourseSection getSection(User currentUser, Long courseId, Long sectionId) {
        getManageableCourse(currentUser, courseId);
        CourseSection section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phần học."));
        if (!section.getCourse().getCourseId().equals(courseId)) {
            throw new ResourceNotFoundException("Phần học không thuộc khóa học này.");
        }
        return section;
    }

    @Transactional
    public CourseSection createSection(
            User currentUser, Long courseId, String sectionTitle, Integer sectionOrder) {
        Course course = getManageableCourse(currentUser, courseId);
        validateSectionInput(courseId, sectionTitle, sectionOrder, null);

        CourseSection section = new CourseSection();
        section.setCourse(course);
        section.setSectionTitle(sectionTitle.trim());
        section.setSectionOrder(sectionOrder);
        return sectionRepository.save(section);
    }

    @Transactional
    public CourseSection updateSection(
            User currentUser, Long courseId, Long sectionId, String sectionTitle, Integer sectionOrder) {
        CourseSection section = getSection(currentUser, courseId, sectionId);
        validateSectionInput(courseId, sectionTitle, sectionOrder, sectionId);

        section.setSectionTitle(sectionTitle.trim());
        section.setSectionOrder(sectionOrder);
        return sectionRepository.save(section);
    }

    @Transactional
    public void deleteSection(User currentUser, Long courseId, Long sectionId) {
        CourseSection section = getSection(currentUser, courseId, sectionId);
        if (lessonRepository.existsBySection_SectionId(sectionId)) {
            throw new IllegalArgumentException(
                    "Không thể xóa phần còn bài học. Hãy chuyển hoặc xóa các bài học trong phần trước.");
        }
        sectionRepository.delete(section);
    }

    private void validateSectionInput(
            Long courseId, String sectionTitle, Integer sectionOrder, Long currentSectionId) {
        if (sectionTitle == null || sectionTitle.trim().isEmpty()) {
            throw new IllegalArgumentException("Vui lòng nhập tên phần.");
        }
        if (sectionTitle.trim().length() > 200) {
            throw new IllegalArgumentException("Tên phần không được vượt quá 200 ký tự.");
        }
        if (sectionOrder == null || sectionOrder < 0) {
            throw new IllegalArgumentException("Thứ tự phần không được âm.");
        }

        boolean duplicated = currentSectionId == null
                ? sectionRepository.existsByCourse_CourseIdAndSectionOrder(courseId, sectionOrder)
                : sectionRepository.existsByCourse_CourseIdAndSectionOrderAndSectionIdNot(
                courseId, sectionOrder, currentSectionId);

        if (duplicated) {
            throw new IllegalArgumentException(
                    "Thứ tự phần này đã tồn tại trong khóa học. Vui lòng chọn số khác.");
        }
    }

    private CourseSection findSectionInCourse(Long courseId, Long sectionId) {
        if (sectionId == null) {
            return null;
        }
        CourseSection section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new IllegalArgumentException("Phần được chọn không tồn tại."));
        if (!section.getCourse().getCourseId().equals(courseId)) {
            throw new IllegalArgumentException("Phần được chọn không thuộc khóa học này.");
        }
        return section;
    }
}
