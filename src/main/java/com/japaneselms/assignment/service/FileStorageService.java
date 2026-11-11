package com.japaneselms.assignment.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    String storeFile(MultipartFile file);
    Resource loadFileAsResource(String storagePath);
    void validateSubmissionFile(MultipartFile file);
    void deleteFile(String storagePath);
}
