package com.japaneselms.learning.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

public class MaterialForm {

    @NotBlank(message = "Vui lòng nhập tên tài liệu.")
    @Size(max = 200, message = "Tên tài liệu không được vượt quá 200 ký tự.")
    private String materialName;

    @NotBlank(message = "Vui lòng chọn loại tài liệu.")
    private String materialType = "PDF";

    @Size(max = 1000, message = "URL không được vượt quá 1000 ký tự.")
    private String externalUrl;

    private MultipartFile file;

    private String existingFileName;

    public String getMaterialName() {
        return materialName;
    }

    public void setMaterialName(String materialName) {
        this.materialName = materialName;
    }

    public String getMaterialType() {
        return materialType;
    }

    public void setMaterialType(String materialType) {
        this.materialType = materialType;
    }

    public String getExternalUrl() {
        return externalUrl;
    }

    public void setExternalUrl(String externalUrl) {
        this.externalUrl = externalUrl;
    }

    public MultipartFile getFile() {
        return file;
    }

    public void setFile(MultipartFile file) {
        this.file = file;
    }

    public String getExistingFileName() {
        return existingFileName;
    }

    public void setExistingFileName(String existingFileName) {
        this.existingFileName = existingFileName;
    }
}
