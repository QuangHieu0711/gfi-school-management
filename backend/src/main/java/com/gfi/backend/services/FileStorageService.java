package com.gfi.backend.services;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Base64;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.gfi.backend.controllers.exceptions.UserMessageException;
import com.gfi.backend.models.dtos.common.FileUploadDto;
import com.gfi.backend.models.global.CommonErrorCode;

@Service
public class FileStorageService {

    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024;
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/gif");

    private final Path uploadRoot;

    public FileStorageService(@Value("${app.upload.dir:uploads}") String uploadDir) {
        this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    public FileUploadDto storeStudentAvatar(MultipartFile file) {
        validateImage(file);
        try {
            String extension = resolveExtension(file.getOriginalFilename(), file.getContentType());
            return storeStudentAvatar(file.getInputStream(), file.getSize(), file.getContentType(), extension);
        } catch (IOException ex) {
            throw new UserMessageException(CommonErrorCode.BAD_REQUEST);
        }
    }

    public String storeStudentAvatarFromDataUrl(String dataUrl) {
        if (dataUrl == null || dataUrl.isBlank()) {
            return null;
        }
        if (!dataUrl.startsWith("data:image/")) {
            return dataUrl.trim();
        }

        int commaIndex = dataUrl.indexOf(',');
        if (commaIndex < 0) {
            throw new UserMessageException(CommonErrorCode.BAD_REQUEST);
        }

        String metadata = dataUrl.substring(0, commaIndex).toLowerCase();
        String base64Content = dataUrl.substring(commaIndex + 1);
        String contentType = resolveContentType(metadata);
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(base64Content);
        } catch (IllegalArgumentException ex) {
            throw new UserMessageException(CommonErrorCode.BAD_REQUEST);
        }

        validateImage(bytes.length, contentType);
        try {
            String extension = resolveExtension(null, contentType);
            return storeStudentAvatar(new ByteArrayInputStream(bytes), bytes.length, contentType, extension).getUrl();
        } catch (IOException ex) {
            throw new UserMessageException(CommonErrorCode.BAD_REQUEST);
        }
    }

    public Path getUploadRoot() {
        return uploadRoot;
    }

    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new UserMessageException(CommonErrorCode.BAD_REQUEST);
        }
        String contentType = file.getContentType();
        validateImage(file.getSize(), contentType);
    }

    private void validateImage(long size, String contentType) {
        if (size > MAX_FILE_SIZE) {
            throw new UserMessageException(CommonErrorCode.BAD_REQUEST);
        }
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new UserMessageException(CommonErrorCode.BAD_REQUEST);
        }
    }

    private FileUploadDto storeStudentAvatar(java.io.InputStream inputStream, long size, String contentType, String extension)
            throws IOException {
        Path targetDirectory = uploadRoot.resolve("students");
        Files.createDirectories(targetDirectory);

        String storedFileName = UUID.randomUUID() + extension;
        Path targetFile = targetDirectory.resolve(storedFileName);

        Files.copy(inputStream, targetFile, StandardCopyOption.REPLACE_EXISTING);

        return FileUploadDto.builder()
                .fileName(storedFileName)
                .url("/uploads/students/" + storedFileName)
                .size(size)
                .contentType(contentType)
                .build();
    }

    private String resolveContentType(String metadata) {
        if (metadata.startsWith("data:image/png")) {
            return "image/png";
        }
        if (metadata.startsWith("data:image/webp")) {
            return "image/webp";
        }
        if (metadata.startsWith("data:image/gif")) {
            return "image/gif";
        }
        return "image/jpeg";
    }

    private String resolveExtension(String originalFileName, String contentType) {
        if (originalFileName != null) {
            int index = originalFileName.lastIndexOf('.');
            if (index >= 0) {
                return originalFileName.substring(index).toLowerCase();
            }
        }
        return switch (contentType == null ? "" : contentType.toLowerCase()) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/gif" -> ".gif";
            default -> ".jpg";
        };
    }
}
