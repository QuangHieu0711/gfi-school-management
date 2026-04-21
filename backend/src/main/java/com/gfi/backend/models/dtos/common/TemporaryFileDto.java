package com.gfi.backend.models.dtos.common;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TemporaryFileDto {
    private String fileName;
    private String contentType;
    private byte[] content;
}
