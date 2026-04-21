package com.gfi.backend.services.implement;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.gfi.backend.controllers.exceptions.UserMessageException;
import com.gfi.backend.models.dtos.common.TemporaryFileDto;
import com.gfi.backend.services.interfaces.ImportErrorFileStorageService;

@Service
public class ImportErrorFileStorageServiceImpl implements ImportErrorFileStorageService {
    private static final Duration TTL = Duration.ofMinutes(15);
    private final Map<String, StoredFile> storage = new ConcurrentHashMap<>();

    @Override
    public String store(String fileName, String contentType, byte[] content) {
        cleanupExpired();
        String token = UUID.randomUUID().toString();
        storage.put(token, new StoredFile(fileName, contentType, content, Instant.now().plus(TTL)));
        return token;
    }

    @Override
    public TemporaryFileDto get(String token) {
        cleanupExpired();
        StoredFile storedFile = storage.remove(token);
        if (storedFile == null || storedFile.expiresAt().isBefore(Instant.now())) {
            throw new UserMessageException("File lỗi import không còn tồn tại hoặc đã hết hạn");
        }
        return TemporaryFileDto.builder()
                .fileName(storedFile.fileName())
                .contentType(storedFile.contentType())
                .content(storedFile.content())
                .build();
    }

    private void cleanupExpired() {
        Instant now = Instant.now();
        storage.entrySet().removeIf(entry -> entry.getValue().expiresAt().isBefore(now));
    }

    private record StoredFile(String fileName, String contentType, byte[] content, Instant expiresAt) {
    }
}
