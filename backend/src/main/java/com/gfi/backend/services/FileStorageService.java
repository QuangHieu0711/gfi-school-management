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

/**
 * Service để quản lý upload/download file sử dụng MinIO
 * 
 * MinIO là một S3-compatible object storage
 * Thay vì lưu file trên local disk, file được upload lên MinIO server
 * 
 */
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

    /**
     * Upload student avatar từ MultipartFile lên MinIO
     * 
     * Quy trình:
     * 1. Validate file (size, content-type)
     * 2. Convert file input stream
     * 3. Upload lên MinIO
     * 4. Return FileUploadDto với URL
     * 
     * @param file file từ client
     * @return FileUploadDto chứa thông tin file vừa upload
     */
    /**
     * Upload student avatar từ MultipartFile lên MinIO với path gọn gàng
     * @param file file upload
     * @param unitName tên đơn vị (vd: Trường Tiểu học Tây Sơn)
     * @param schoolYear tên năm học (vd: 2025-2026)
     */
    public FileUploadDto storeStudentAvatar(MultipartFile file, String unitName, String schoolYear) {
        validateImage(file);
        try {
            String normalizedContentType = normalizeContentType(file.getContentType());
            String extension = resolveExtension(file.getOriginalFilename(), normalizedContentType);
            return storeStudentAvatar(file.getInputStream(), file.getSize(), normalizedContentType, extension, unitName, schoolYear);
        } catch (IOException ex) {
            log.error("Error reading file input stream", ex);
            throw new UserMessageException(CommonErrorCode.BAD_REQUEST);
        }
    }

    /**
     * Upload student avatar từ Data URL (base64) lên MinIO
     * 
     * Quy trình:
     * 1. Parse Data URL để lấy content-type và base64 data
     * 2. Decode base64 thành bytes
     * 3. Validate image
     * 4. Upload lên MinIO
     * 5. Return URL để access file
     * 
     * Nếu dataUrl là null/blank hoặc không phải data URL, trả về nguyên đó
     * (có thể là URL từ previous upload)
     * 
     * @param dataUrl data URL từ client (ví dụ: "data:image/png;base64,...")
     * @return URL của file trên MinIO hoặc dataUrl gốc nếu không phải data URL
     */
    /**
     * Upload student avatar từ Data URL (base64) lên MinIO với path gọn gàng
     * @param dataUrl data URL
     * @param unitName tên đơn vị (vd: Trường Tiểu học Tây Sơn)
     * @param schoolYear tên năm học (vd: 2025-2026)
     */
    public String storeStudentAvatarFromDataUrl(String dataUrl, String unitName, String schoolYear) {
        if (dataUrl == null || dataUrl.isBlank()) {
            return null;
        }
        if (!dataUrl.startsWith("data:image/")) {
            // Nếu không phải data URL, trả về nguyên đó (có thể là URL cũ)
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
            return storeStudentAvatar(new ByteArrayInputStream(bytes), bytes.length, contentType, extension, unitName, schoolYear).getUrl();
        } catch (IOException ex) {
            log.error("Error uploading avatar from data URL", ex);
            throw new UserMessageException(CommonErrorCode.BAD_REQUEST);
        }
    }

    /**
     * Kiểm tra file có hợp lệ không
     * - File không được null/empty
     * - Kích thước <= 5MB
     * - Content-type phải là image/jpeg, image/png, image/webp hoặc image/gif
     */
    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new UserMessageException(CommonErrorCode.BAD_REQUEST);
        }
        String contentType = normalizeContentType(file.getContentType());
        validateImage(file.getSize(), contentType);
    }

    /**
     * Validate kích thước và content-type
     */
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

    /**
     * Tạo bucket nếu chưa tồn tại
     * 
     * Giải thích:
     * - bucketExists(): Check xem bucket có tồn tại không
     * - makeBucket(): Tạo bucket mới
     * 
     * Các exception từ MinIO:
     * - InvalidKeyException: Access key hoặc secret key sai
     * - IOException: Network error
     * - NoSuchAlgorithmException: Encryption algorithm không tìm thấy
     * Vì vậy catch Exception để bao gồm tất cả
     */
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
            log.error("Lỗi khi kiểm tra/tạo bucket", ex);
            throw new RuntimeException("Thất bại khi thực hiện thao tác bucket", ex);
        }
    }

    /**
     * Upload file input stream lên MinIO
     * 
     * Quy trình upload:
     * 1. Tạo bucket nếu chưa tồn tại
     * 2. Tạo tên file: {uuid}.{extension}
     * 3. Tạo object path trong bucket: {folder}/{fileName}
     * 4. Upload file lên MinIO
     * 5. Return thông tin file vừa upload
     * 
     * @param inputStream input stream của file
     * @param size kích thước file (bytes)
     * @param contentType MIME type của file
     * @param extension đuôi file (ví dụ: .jpg, .png)
     * @return FileUploadDto chứa: fileName, URL, size, contentType
     * @throws IOException nếu có lỗi khi upload
     */
    /**
     * Core upload logic với path gọn gàng
     * Path: students/{unitName}/{schoolYear}/{fileName}
     * Ví dụ: students/Trường Tiểu học Tây Sơn/2025-2026/uuid.jpg
     */
    private FileUploadDto storeStudentAvatar(java.io.InputStream inputStream, long size, String contentType, String extension, String unitName, String schoolYear)
            throws IOException {
        // Bước 1: Tạo bucket nếu chưa tồn tại
        ensureBucketExists();
        // Bước 2: Tạo tên file unique
        String storedFileName = UUID.randomUUID() + extension;
        // Bước 3: Tạo full path: students/{unitName}/{schoolYear}/{fileName}
        String objectName = String.format("students/%s/%s/%s", unitName, schoolYear, storedFileName);
        try {
            // Bước 4: Upload file lên MinIO
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .object(objectName)
                            .stream(inputStream, size, -1)
                            .contentType(contentType)
                            .build());
            log.info("File uploaded to MinIO: {}/{}", minioProperties.getBucketName(), objectName);
            // Bước 5: Tạo public URL để access file
            String fileUrl = minioProperties.getUrl() + "/" + minioProperties.getBucketName() + "/" + objectName;
            return FileUploadDto.builder()
                    .fileName(storedFileName)
                    .url(fileUrl)
                    .size(size)
                    .contentType(contentType)
                    .build();
        } catch (Exception ex) {
            log.error("Lỗi khi upload file lên MinIO", ex);
            throw new IOException("Thất bại khi upload file lên MinIO", ex);
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
