package com.gfi.backend.models.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

/**
 * Thuộc tính cấu hình MinIO
 * Đọc từ application.yml với prefix: minio
 * Ví dụ:
 *   minio:
 *     url: http://localhost:9000
 *     accessKey: minioadmin
 *     secretKey: minioadmin
 *     bucketName: student-avatars
 */
@Component
@ConfigurationProperties(prefix = "minio")
@Getter
@Setter
public class MinioProperties {
    
    /**
     * URL của MinIO server
     * Ví dụ: http://localhost:9000 hoặc https://minio.example.com
     */
    private String url;
    
    /**
     * Access key (username) để authenticate với MinIO
     */
    private String accessKey;
    
    /**
     * Secret key (password) để authenticate với MinIO
     */
    private String secretKey;
    
    /**
     * Tên bucket dùng để lưu student avatar
     */
    private String bucketName = "student-avatars";
    
    /**
     * Tên folder trong bucket để lưu file
     * Ví dụ: "students" sẽ tạo path: student-avatars/students/filename
     */
    private String folderName = "students";
}
