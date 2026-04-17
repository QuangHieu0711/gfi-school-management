package com.gfi.backend.services;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.gfi.backend.controllers.exceptions.UserMessageException;
import com.gfi.backend.models.configuration.MinioProperties;
import com.gfi.backend.models.dtos.common.FileUploadDto;
import com.gfi.backend.models.global.CommonErrorCode;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileStorageService {

    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024;
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/webp",
            "image/gif");

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    public FileUploadDto storeStudentAvatar(MultipartFile file, String unitName, String schoolYear) {
        validateImage(file);
        try {
            String normalizedContentType = normalizeContentType(file.getContentType());
            String extension = resolveExtension(file.getOriginalFilename(), normalizedContentType);
            return storeImage(file.getInputStream(), file.getSize(), normalizedContentType, extension,
                    String.format("students/%s/%s", unitName, schoolYear));
        } catch (IOException ex) {
            log.error("Error reading file input stream", ex);
            throw new UserMessageException(CommonErrorCode.BAD_REQUEST);
        }
    }

    public String storeStudentAvatarFromDataUrl(String dataUrl, String unitName, String schoolYear) {
        return storeImageFromDataUrl(dataUrl, String.format("students/%s/%s", unitName, schoolYear));
    }

    public String storeStaffAvatarFromDataUrl(String dataUrl, String unitName, String yearLabel) {
        return storeImageFromDataUrl(dataUrl, String.format("staffs/%s/%s/avatar", unitName, yearLabel));
    }

    public String storeStaffSignatureFromDataUrl(String dataUrl, String unitName, String yearLabel) {
        return storeImageFromDataUrl(dataUrl, String.format("staffs/%s/%s/signature", unitName, yearLabel));
    }

    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new UserMessageException(CommonErrorCode.BAD_REQUEST);
        }
        String contentType = normalizeContentType(file.getContentType());
        validateImage(file.getSize(), contentType);
    }

    private void validateImage(long size, String contentType) {
        if (size > MAX_FILE_SIZE) {
            log.warn("File size exceeds limit: {} > {}", size, MAX_FILE_SIZE);
            throw new UserMessageException(CommonErrorCode.BAD_REQUEST);
        }
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            log.warn("Invalid content type: {}", contentType);
            throw new UserMessageException(CommonErrorCode.BAD_REQUEST);
        }
    }

    private String storeImageFromDataUrl(String dataUrl, String folderPath) {
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
            log.error("Invalid base64 content", ex);
            throw new UserMessageException(CommonErrorCode.BAD_REQUEST);
        }

        validateImage(bytes.length, contentType);
        try {
            String extension = resolveExtension(null, contentType);
            return storeImage(new ByteArrayInputStream(bytes), bytes.length, contentType, extension, folderPath)
                    .getUrl();
        } catch (IOException ex) {
            log.error("Error uploading image from data URL", ex);
            throw new UserMessageException(CommonErrorCode.BAD_REQUEST);
        }
    }

    private void ensureBucketExists() {
        try {
            boolean exists = minioClient.bucketExists(io.minio.BucketExistsArgs.builder()
                    .bucket(minioProperties.getBucketName())
                    .build());

            if (!exists) {
                minioClient.makeBucket(io.minio.MakeBucketArgs.builder()
                        .bucket(minioProperties.getBucketName())
                        .build());
                log.info("Bucket created: {}", minioProperties.getBucketName());
            }
        } catch (Exception ex) {
            log.error("Loi khi kiem tra/tao bucket", ex);
            throw new RuntimeException("That bai khi thuc hien thao tac bucket", ex);
        }
    }

    private FileUploadDto storeImage(java.io.InputStream inputStream, long size, String contentType, String extension,
            String folderPath) throws IOException {
        ensureBucketExists();
        String storedFileName = UUID.randomUUID() + extension;
        String objectName = String.format("%s/%s", folderPath, storedFileName);
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .object(objectName)
                            .stream(inputStream, size, -1)
                            .contentType(contentType)
                            .build());
            log.info("File uploaded to MinIO: {}/{}", minioProperties.getBucketName(), objectName);
            String fileUrl = minioProperties.getUrl() + "/" + minioProperties.getBucketName() + "/" + objectName;
            return FileUploadDto.builder()
                    .fileName(storedFileName)
                    .url(fileUrl)
                    .size(size)
                    .contentType(contentType)
                    .build();
        } catch (Exception ex) {
            log.error("Loi khi upload file len MinIO", ex);
            throw new IOException("That bai khi upload file len MinIO", ex);
        }
    }

    private String resolveContentType(String metadata) {
        if (metadata.startsWith("data:image/png")) {
            return "image/png";
        }
        if (metadata.startsWith("data:image/jpg")) {
            return "image/jpeg";
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

    private String normalizeContentType(String contentType) {
        if (contentType == null) {
            return null;
        }

        return switch (contentType.toLowerCase()) {
            case "image/jpg" -> "image/jpeg";
            default -> contentType.toLowerCase();
        };
    }
}
