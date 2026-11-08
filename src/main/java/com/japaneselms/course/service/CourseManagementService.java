package com.japaneselms.course.service;

import com.japaneselms.course.dto.CourseForm;
import com.japaneselms.entity.Course;
import com.japaneselms.entity.User;
import com.japaneselms.learning.service.LearningMaterialService;
import com.japaneselms.learning.service.LessonService;
import com.japaneselms.repository.*;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Service
public class CourseManagementService {

    private static final long MAX_IMAGE_SIZE = 5L * 1024 * 1024; // 5MB

    private final CourseRepository courseRepository;
    private final CourseSectionRepository sectionRepository;
    private final LessonRepository lessonRepository;
    private final LearningMaterialRepository materialRepository;
    private final LessonCompletionRepository completionRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AssignmentRepository assignmentRepository;
    private final LessonService lessonService;
    private final LearningMaterialService materialService;
    private final Path imageRootDir;

    public CourseManagementService(
            CourseRepository courseRepository,
            CourseSectionRepository sectionRepository,
            LessonRepository lessonRepository,
            LearningMaterialRepository materialRepository,
            LessonCompletionRepository completionRepository,
            EnrollmentRepository enrollmentRepository,
            AssignmentRepository assignmentRepository,
            LessonService lessonService,
            LearningMaterialService materialService,
            @Value("${app.upload.course-images-dir:uploads/course-images}") String courseImagesDir) {
        this.courseRepository = courseRepository;
        this.sectionRepository = sectionRepository;
        this.lessonRepository = lessonRepository;
        this.materialRepository = materialRepository;
        this.completionRepository = completionRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.assignmentRepository = assignmentRepository;
        this.lessonService = lessonService;
        this.materialService = materialService;
        this.imageRootDir = Paths.get(courseImagesDir).toAbsolutePath().normalize();
    }

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(imageRootDir);
        } catch (IOException e) {
            throw new IllegalStateException("Không thể tạo thư mục lưu ảnh khóa học.", e);
        }
    }

    @Transactional
    public Course createCourse(User currentUser, CourseForm form) {
        validateCoverImage(form.getCoverImage());

        Course course = new Course();
        course.setInstructor(currentUser);
        course.setCourseStatus(CourseStatus.ACTIVE);
        applyForm(course, form);
        course = courseRepository.save(course);

        if (form.getCoverImage() != null && !form.getCoverImage().isEmpty()) {
            course.setCoverImagePath(saveCoverImage(course.getCourseId(), form.getCoverImage()));
        }
        return course;
    }

    @Transactional
    public Course updateCourse(User currentUser, Long courseId, CourseForm form) {
        Course course = lessonService.getManageableCourse(currentUser, courseId);
        validateCoverImage(form.getCoverImage());

        applyForm(course, form);

        String oldImage = null;
        if (form.getCoverImage() != null && !form.getCoverImage().isEmpty()) {
            oldImage = course.getCoverImagePath();
            course.setCoverImagePath(saveCoverImage(courseId, form.getCoverImage()));
        }
        courseRepository.save(course);

        if (oldImage != null) {
            deleteCoverImage(oldImage);
        }
        return course;
    }
    /**
     * Cap nhat rieng anh bia khoa hoc.
     * Khong thay doi thong tin khoa hoc khac.
     */
    @Transactional
    public void updateCourseImage(User currentUser, Long courseId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return;
        }

        Course course = lessonService.getManageableCourse(currentUser, courseId);

        validateCoverImage(file);

        String oldImage = course.getCoverImagePath();
        String newImage = saveCoverImage(courseId, file);

        course.setCoverImagePath(newImage);
        courseRepository.save(course);

        if (oldImage != null) {
            deleteCoverImage(oldImage);
        }
    }

    @Transactional(readOnly = true)
    public CourseForm getForm(User currentUser, Long courseId) {
        Course course = lessonService.getManageableCourse(currentUser, courseId);
        CourseForm form = new CourseForm();
        form.setCourseName(course.getCourseName());
        form.setJlptLevel(course.getJlptLevel());
        form.setDescription(course.getDescription());
        return form;
    }

    @Transactional
    public void deleteCourse(User currentUser, Long courseId) {
        Course course = lessonService.getManageableCourse(currentUser, courseId);
        if (assignmentRepository.existsByCourse_CourseId(courseId)) {
            throw new IllegalArgumentException(
                    "Không thể xóa khóa học vì đã có bài tập thuộc khóa học này.");
        }

        List<String> materialFiles = materialRepository.findStoragePathsByCourseId(courseId);
        String coverImage = course.getCoverImagePath();

        completionRepository.deleteAllByCourseId(courseId);
        materialRepository.deleteAllByCourseId(courseId);
        lessonRepository.deleteAllByCourseId(courseId);
        sectionRepository.deleteAllByCourseId(courseId);
        enrollmentRepository.deleteAllByCourseId(courseId);
        courseRepository.deleteById(courseId);

        for (String path : materialFiles) {
            materialService.deleteFile(path);
        }
        deleteCoverImage(coverImage);
    }

    @Transactional(readOnly = true)
    public ResponseEntity<Resource> getCourseImageResponse(User currentUser, Long courseId) {
        Course course = lessonService.getViewableCourse(currentUser, courseId);
        String storagePath = course.getCoverImagePath();
        if (storagePath == null || storagePath.isBlank()) {
            return ResponseEntity.notFound().build();
        }

        Path path = imageRootDir.resolve(storagePath).normalize();
        if (!Files.exists(path) || !Files.isRegularFile(path)) {
            return ResponseEntity.notFound().build();
        }

        String lower = storagePath.toLowerCase();
        String contentType = "image/jpeg";
        if (lower.endsWith(".png")) {
            contentType = "image/png";
        } else if (lower.endsWith(".webp")) {
            contentType = "image/webp";
        } else if (lower.endsWith(".gif")) {
            contentType = "image/gif";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .body(new FileSystemResource(path));
    }

    private void applyForm(Course course, CourseForm form) {
        course.setCourseName(form.getCourseName().trim());
        course.setJlptLevel(form.getJlptLevel().trim().toUpperCase());
        course.setDescription(form.getDescription().trim());
    }

    private void validateCoverImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return;
        }
        if (file.getSize() > MAX_IMAGE_SIZE) {
            throw new IllegalArgumentException("Ảnh nền không được vượt quá 5MB.");
        }
        String ext = getExtension(file.getOriginalFilename());
        if (!".jpg".equals(ext) && !".jpeg".equals(ext)
                && !".png".equals(ext) && !".webp".equals(ext) && !".gif".equals(ext)) {
            throw new IllegalArgumentException(
                    "Ảnh nền phải có định dạng JPG, PNG, WEBP hoặc GIF.");
        }
    }

    private String saveCoverImage(Long courseId, MultipartFile file) {
        String ext = getExtension(file.getOriginalFilename());
        Path folder = imageRootDir.resolve(String.valueOf(courseId));
        try {
            Files.createDirectories(folder);
            String storedName = UUID.randomUUID() + ext;
            Path target = folder.resolve(storedName).normalize();
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target);
            }
            return courseId + "/" + storedName;
        } catch (IOException e) {
            throw new IllegalStateException("Không thể lưu ảnh nền khóa học.", e);
        }
    }

    private void deleteCoverImage(String storagePath) {
        if (storagePath == null || storagePath.isBlank()) {
            return;
        }
        try {
            Path path = imageRootDir.resolve(storagePath).normalize();
            Files.deleteIfExists(path);
        } catch (Exception ignored) {
        }
    }

    private String getExtension(String fileName) {
        if (fileName == null) {
            return "";
        }
        String lower = fileName.toLowerCase();
        int dot = lower.lastIndexOf('.');
        return dot >= 0 ? lower.substring(dot) : "";
    }
}
