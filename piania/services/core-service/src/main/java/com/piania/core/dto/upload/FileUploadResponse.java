package com.piania.core.dto.upload;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FileUploadResponse {
    private String url;
    private String fileName;
}
