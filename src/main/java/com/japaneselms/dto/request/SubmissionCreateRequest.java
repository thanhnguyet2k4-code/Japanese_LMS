package com.japaneselms.dto.request;

import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

public class SubmissionCreateRequest {

    @NotNull(message = "Mã bài tập không được để trống")
    private Long assignmentId;

    private String responseText;

    private MultipartFile file;

    public SubmissionCreateRequest() {
    }

    public boolean hasContent() {
        boolean hasText = responseText != null && !responseText.trim().isEmpty();
        boolean hasUpload = file != null && !file.isEmpty();
        return hasText || hasUpload;
    }

    public Long getAssignmentId() {
        return assignmentId;
    }

    public void setAssignmentId(Long assignmentId) {
        this.assignmentId = assignmentId;
    }

    public String getResponseText() {
        return responseText;
    }

    public void setResponseText(String responseText) {
        this.responseText = responseText;
    }

    public MultipartFile getFile() {
        return file;
    }

    public void setFile(MultipartFile file) {
        this.file = file;
    }
}
