package com.japaneselms.service.impl;

import com.japaneselms.exception.BusinessException;
import com.japaneselms.service.FileStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class FileStorageServiceImpl implements FileStorageService {

    private final Path fileStorageLocation;
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("pdf", "docx", "png", "mp3");
    private static final long MAX_FILE_SIZE = 20 * 1024 * 1024; // 20 MB

    public FileStorageServiceImpl(@Value("${app.upload.dir:uploads/submissions}") String uploadDir) {
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            throw new BusinessException("Không thể tạo thư mục lưu trữ file nộp bài: " + ex.getMessage());
        }
    }

    @Override
    public void validateSubmissionFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return;
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException("Dung lượng file vượt quá giới hạn 20MB.");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.contains(".")) {
            throw new BusinessException("Tên file không hợp lệ hoặc thiếu phần mở rộng.");
        }

        String extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessException("Định dạng file không được hỗ trợ. Hệ thống chỉ cho phép: PDF, DOCX, PNG, MP3.");
        }
    }

    @Override
    public String storeFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        validateSubmissionFile(file);

        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename() != null ? file.getOriginalFilename() : "file");
        String extension = "";
        if (originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        String storedFileName = UUID.randomUUID().toString() + extension;

        try {
            if (storedFileName.contains("..")) {
                throw new BusinessException("Tên file không hợp lệ: " + storedFileName);
            }

            Path targetLocation = this.fileStorageLocation.resolve(storedFileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            return targetLocation.toString();
        } catch (IOException ex) {
            throw new BusinessException("Không thể lưu trữ file: " + ex.getMessage());
        }
    }

    @Override
    public Resource loadFileAsResource(String storagePath) {
        try {
            Path filePath = Paths.get(storagePath).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new BusinessException("Tệp đính kèm không tồn tại trên hệ thống lưu trữ, vui lòng liên hệ quản trị viên.");
            }
        } catch (MalformedURLException ex) {
            throw new BusinessException("Đường dẫn file không hợp lệ: " + ex.getMessage());
        }
    }
}
