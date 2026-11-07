package com.japaneselms.learning.service;

import com.japaneselms.common.exception.ResourceNotFoundException;
import com.japaneselms.entity.LearningMaterial;
import com.japaneselms.entity.Lesson;
import com.japaneselms.entity.User;
import com.japaneselms.learning.dto.MaterialForm;
import com.japaneselms.repository.LearningMaterialRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
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
public class LearningMaterialService {

    private static final long MAX_FILE_SIZE = 20L * 1024 * 1024; // 20MB

    private final LearningMaterialRepository materialRepository;
    private final LessonService lessonService;
    private final Path rootDirectory;

    public LearningMaterialService(
            LearningMaterialRepository materialRepository,
            LessonService lessonService,
            @Value("${app.upload.materials-dir:uploads/materials}") String materialsDir) {
        this.materialRepository = materialRepository;
        this.lessonService = lessonService;
        this.rootDirectory = Paths.get(materialsDir).toAbsolutePath().normalize();
    }

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(rootDirectory);
        } catch (IOException e) {
            throw new IllegalStateException("Không thể tạo thư mục lưu tài liệu.", e);
        }
    }

    // Lay danh sach tai lieu cua bai hoc (gan link nhung YouTube neu co)
    @Transactional(readOnly = true)
    public List<LearningMaterial> listMaterialsByLesson(Long lessonId) {
        List<LearningMaterial> materials =
                materialRepository.findByLesson_LessonIdOrderByCreatedAtAsc(lessonId);
        for (LearningMaterial material : materials) {
            if ("YOUTUBE".equalsIgnoreCase(material.getMaterialType())) {
                material.setYoutubeEmbedUrl(toYoutubeEmbedUrl(material.getExternalUrl()));
            }
        }
        return materials;
    }

    @Transactional(readOnly = true)
    public MaterialForm getMaterialForm(User currentUser, Long lessonId, Long materialId) {
        LearningMaterial material = getManageableMaterial(currentUser, lessonId, materialId);
        MaterialForm form = new MaterialForm();
        form.setMaterialName(material.getMaterialName());
        form.setMaterialType(material.getMaterialType());
        form.setExternalUrl(material.getExternalUrl());
        form.setExistingFileName(material.getOriginalFileName());
        return form;
    }

    // S12: Them tai lieu moi (PDF, MP3 hoac YouTube)
    @Transactional
    public void addMaterial(User currentUser, Long lessonId, MaterialForm form) {
        Lesson lesson = lessonService.getManageableLesson(currentUser, lessonId);
        String type = normalizeType(form.getMaterialType());

        LearningMaterial material = new LearningMaterial();
        material.setLesson(lesson);
        material.setMaterialName(form.getMaterialName().trim());
        material.setMaterialType(type);
        material.setUploadedBy(currentUser);

        if ("YOUTUBE".equals(type)) {
            applyYoutubeUrl(material, form.getExternalUrl());
        } else {
            MultipartFile file = form.getFile();
            if (file == null || file.isEmpty()) {
                throw new IllegalArgumentException("Vui lòng chọn tệp PDF hoặc MP3.");
            }
            saveUploadedFile(material, lesson, file, type);
        }

        materialRepository.save(material);
    }

    // S12: Cap nhat tai lieu
    @Transactional
    public void updateMaterial(
            User currentUser, Long lessonId, Long materialId, MaterialForm form) {
        LearningMaterial material = getManageableMaterial(currentUser, lessonId, materialId);
        Lesson lesson = material.getLesson();
        String oldType = material.getMaterialType();
        String newType = normalizeType(form.getMaterialType());
        String oldStoragePath = material.getStoragePath();

        material.setMaterialName(form.getMaterialName().trim());
        material.setMaterialType(newType);

        if ("YOUTUBE".equals(newType)) {
            applyYoutubeUrl(material, form.getExternalUrl());
        } else {
            MultipartFile file = form.getFile();
            if (file != null && !file.isEmpty()) {
                saveUploadedFile(material, lesson, file, newType);
            } else {
                // Neu doi loai tai lieu hoac truoc do chua co file thi bat buoc chon file moi
                if (oldStoragePath == null || oldStoragePath.isBlank()
                        || !newType.equalsIgnoreCase(oldType)) {
                    throw new IllegalArgumentException(
                            "Vui lòng chọn tệp " + newType + " mới khi thay đổi loại tài liệu.");
                }
            }
        }

        materialRepository.save(material);

        // Xoa file cu neu da thay bang file moi hoac chuyen sang YouTube
        if (oldStoragePath != null && !oldStoragePath.equals(material.getStoragePath())) {
            deleteFile(oldStoragePath);
        }
    }

    // S12: Xoa tai lieu
    @Transactional
    public Long deleteMaterial(User currentUser, Long materialId) {
        LearningMaterial material = getManageableMaterial(currentUser, null, materialId);
        Long lessonId = material.getLesson().getLessonId();
        String storagePath = material.getStoragePath();

        materialRepository.delete(material);
        deleteFile(storagePath);
        return lessonId;
    }

    // Tai hoac xem truc tiep file PDF / MP3 (co kiem tra quyen)
    @Transactional(readOnly = true)
    public ResponseEntity<Resource> downloadForUser(User currentUser, Long materialId) {
        LearningMaterial material = materialRepository.findWithLessonAndCourseById(materialId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài liệu."));

        // Kiem tra nguoi dung co quyen xem bai hoc chua tai lieu nay khong
        lessonService.getViewableLesson(currentUser, material.getLesson().getLessonId());

        if ("YOUTUBE".equalsIgnoreCase(material.getMaterialType())) {
            throw new IllegalArgumentException("Tài liệu YouTube không hỗ trợ tải tệp.");
        }

        if (material.getStoragePath() == null || material.getStoragePath().isBlank()) {
            throw new ResourceNotFoundException("Tài liệu không có tệp đính kèm.");
        }

        Path filePath = rootDirectory.resolve(material.getStoragePath()).normalize();
        if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
            throw new ResourceNotFoundException("Không tìm thấy tệp tài liệu trên máy chủ.");
        }

        MediaType mediaType = "MP3".equalsIgnoreCase(material.getMaterialType())
                ? MediaType.parseMediaType("audio/mpeg")
                : MediaType.APPLICATION_PDF;

        String fileName = material.getOriginalFileName() != null
                ? material.getOriginalFileName()
                : filePath.getFileName().toString();

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + fileName.replace("\"", "") + "\"")
                .body(new FileSystemResource(filePath));
    }

    // ==================== CAC HAM HO TRO KIEM TRA VA LUU FILE ====================

    private LearningMaterial getManageableMaterial(
            User currentUser, Long lessonId, Long materialId) {
        LearningMaterial material = materialRepository.findWithLessonAndCourseById(materialId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài liệu."));

        lessonService.getManageableLesson(currentUser, material.getLesson().getLessonId());

        if (lessonId != null && !material.getLesson().getLessonId().equals(lessonId)) {
            throw new ResourceNotFoundException("Không tìm thấy tài liệu trong bài học này.");
        }
        return material;
    }

    private String normalizeType(String type) {
        if (type == null) {
            throw new IllegalArgumentException("Vui lòng chọn loại tài liệu.");
        }
        String normalized = type.trim().toUpperCase();
        if (!"PDF".equals(normalized) && !"MP3".equals(normalized) && !"YOUTUBE".equals(normalized)) {
            throw new IllegalArgumentException("Loại tài liệu chỉ được là PDF, MP3 hoặc YOUTUBE.");
        }
        return normalized;
    }

    private void applyYoutubeUrl(LearningMaterial material, String externalUrl) {
        if (externalUrl == null || externalUrl.isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập đường dẫn video YouTube.");
        }
        String videoId = extractYoutubeId(externalUrl);
        if (videoId == null) {
            throw new IllegalArgumentException(
                    "Đường dẫn YouTube không hợp lệ. Hãy dùng link youtube.com hoặc youtu.be.");
        }
        material.setExternalUrl(externalUrl.trim());
        material.setStoragePath(null);
        material.setOriginalFileName(null);
    }

    private void saveUploadedFile(
            LearningMaterial material, Lesson lesson, MultipartFile file, String type) {
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("Tệp tải lên không được vượt quá 20MB.");
        }

        String originalName = file.getOriginalFilename() == null
                ? ""
                : file.getOriginalFilename().toLowerCase();

        if ("PDF".equals(type) && !originalName.endsWith(".pdf")) {
            throw new IllegalArgumentException("Tài liệu PDF phải có đuôi .pdf.");
        }
        if ("MP3".equals(type) && !originalName.endsWith(".mp3")) {
            throw new IllegalArgumentException("Tài liệu nghe phải có đuôi .mp3.");
        }

        String extension = "PDF".equals(type) ? ".pdf" : ".mp3";
        Long courseId = lesson.getCourse().getCourseId();
        Long lessonId = lesson.getLessonId();

        Path folder = rootDirectory
                .resolve(String.valueOf(courseId))
                .resolve(String.valueOf(lessonId));

        try {
            Files.createDirectories(folder);
            String storedFileName = UUID.randomUUID() + extension;
            Path targetPath = folder.resolve(storedFileName).normalize();
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, targetPath);
            }

            material.setStoragePath(courseId + "/" + lessonId + "/" + storedFileName);
            material.setOriginalFileName(file.getOriginalFilename());
            material.setExternalUrl(null);
        } catch (IOException e) {
            throw new IllegalStateException("Không thể lưu tệp tài liệu lên máy chủ.", e);
        }
    }

    public void deleteFile(String storagePath) {
        if (storagePath == null || storagePath.isBlank()) {
            return;
        }
        try {
            Path path = rootDirectory.resolve(storagePath).normalize();
            Files.deleteIfExists(path);
        } catch (Exception ignored) {
        }
    }

    // Tach ma video 11 ky tu tu link YouTube (watch?v=..., youtu.be/..., /embed/...)
    public String extractYoutubeId(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        String cleanUrl = url.trim();
        String videoId = null;

        if (cleanUrl.contains("v=")) {
            int start = cleanUrl.indexOf("v=") + 2;
            int end = cleanUrl.indexOf("&", start);
            videoId = end == -1 ? cleanUrl.substring(start) : cleanUrl.substring(start, end);
        } else if (cleanUrl.contains("youtu.be/")) {
            int start = cleanUrl.indexOf("youtu.be/") + 9;
            int end = cleanUrl.indexOf("?", start);
            videoId = end == -1 ? cleanUrl.substring(start) : cleanUrl.substring(start, end);
        } else if (cleanUrl.contains("/embed/")) {
            int start = cleanUrl.indexOf("/embed/") + 7;
            int end = cleanUrl.indexOf("?", start);
            videoId = end == -1 ? cleanUrl.substring(start) : cleanUrl.substring(start, end);
        }

        if (videoId != null && videoId.length() == 11) {
            return videoId;
        }
        return null;
    }

    public String toYoutubeEmbedUrl(String url) {
        String videoId = extractYoutubeId(url);
        if (videoId == null) {
            return null;
        }
        return "https://www.youtube.com/embed/" + videoId;
    }
}
