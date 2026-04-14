package com.gfi.backend.models.configuration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Cấu hình MinIO Client cho Spring Boot
 * 
 * Mục đích:
 * - Khởi tạo MinioClient bean sử dụng cấu hình từ MinioProperties
 * - MinioClient được dùng để tương tác với MinIO server
 * 
 * Cách hoạt động:
 * 1. Spring đọc cấu hình từ application.yml (prefix: minio)
 * 2. MinioProperties được populate với các giá trị từ yaml
 * 3. MinioConfig sử dụng MinioProperties để tạo MinioClient
 * 4. Các class khác inject MinioClient để upload/download file
 */
@Configuration
@EnableConfigurationProperties(MinioProperties.class)
@RequiredArgsConstructor
@Slf4j
public class MinioConfig {

    private final MinioProperties minioProperties;

    /**
     * Tạo và cấu hình MinioClient bean
     * 
     * Giải thích:
     * - MinioClient.builder() bắt đầu setup
     * - endpoint(): URL của MinIO server
     * - credentials(): Access key và Secret key để authenticate
     * - build(): Hoàn thành khởi tạo client
     * 
     * @return MinioClient đã được cấu hình
     */
    @Bean
    public MinioClient minioClient() {
        // Validate properties before creating client
        if (minioProperties.getUrl() == null || minioProperties.getUrl().isBlank()) {
            log.error("Lỗi cấu hình MinIO: URL không được cấu hình. Vui lòng thêm 'minio.url' vào application.yml");
            throw new RuntimeException("Lỗi cấu hình MinIO: URL không được cấu hình");
        }
        if (minioProperties.getAccessKey() == null || minioProperties.getAccessKey().isBlank()) {
            log.error("Lỗi cấu hình MinIO: Access Key không được cấu hình. Vui lòng thêm 'minio.accessKey' vào application.yml");
            throw new RuntimeException("Lỗi cấu hình MinIO: Access Key không được cấu hình");
        }
        if (minioProperties.getSecretKey() == null || minioProperties.getSecretKey().isBlank()) {
            log.error("Lỗi cấu hình MinIO: Secret Key không được cấu hình. Vui lòng thêm 'minio.secretKey' vào application.yml");
            throw new RuntimeException("Lỗi cấu hình MinIO: Secret Key không được cấu hình");
        }

        try {
            MinioClient client = MinioClient.builder()
                    .endpoint(minioProperties.getUrl())
                    .credentials(minioProperties.getAccessKey(), minioProperties.getSecretKey())
                    .build();
            
            log.info("✓ MinIO client initialized successfully at {}", minioProperties.getUrl());
            log.info("✓ Bucket name: {}", minioProperties.getBucketName());
            log.info("✓ Folder name: {}", minioProperties.getFolderName());
            return client;
        } catch (Exception e) {
            log.error("Thất bại khi khởi tạo MinIO client", e);
            throw new RuntimeException("Lỗi khởi tạo MinIO client: " + e.getMessage(), e);
        }
    }
}
