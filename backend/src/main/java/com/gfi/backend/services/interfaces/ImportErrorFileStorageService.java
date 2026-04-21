package com.gfi.backend.services.interfaces;

import com.gfi.backend.models.dtos.common.TemporaryFileDto;

public interface ImportErrorFileStorageService {
    String store(String fileName, String contentType, byte[] content);
    TemporaryFileDto get(String token);
}
