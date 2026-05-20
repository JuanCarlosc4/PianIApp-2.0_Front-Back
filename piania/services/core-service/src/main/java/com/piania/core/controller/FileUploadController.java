package com.piania.core.controller;

import com.piania.core.dto.upload.FileUploadResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@RestController
@RequestMapping("/piania/uploads")
@RequiredArgsConstructor
public class FileUploadController {

    @Value("${piania.uploads.dir:uploads}")
    private String uploadsDir;

    /**
     * Subida simple de ficheros para desarrollo:
     * - Guarda en disco local del contenedor (montable como volumen)
     * - Devuelve una URL servida por core-service (resource handler /uploads/**)
     *
     * Nota: En producción conviene S3/MinIO + presigned URLs.
     */
    @PostMapping(consumes = {"multipart/form-data"})
    @PreAuthorize("hasAnyRole('USER','TEACHER','ADMIN')")
    public FileUploadResponse upload(
            @RequestPart("file") MultipartFile file,
            Authentication authentication
    ) throws IOException {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is required");
        }

        String originalName = StringUtils.cleanPath(file.getOriginalFilename() == null ? "file" : file.getOriginalFilename());
        String ext = "";
        int idx = originalName.lastIndexOf('.');
        if (idx >= 0) ext = originalName.substring(idx);

        String safeName = UUID.randomUUID() + ext;

        Path baseDir = Paths.get(uploadsDir).toAbsolutePath().normalize();
        Files.createDirectories(baseDir);

        Path target = baseDir.resolve(safeName);
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

        return new FileUploadResponse("/uploads/" + safeName, safeName);
    }
}
