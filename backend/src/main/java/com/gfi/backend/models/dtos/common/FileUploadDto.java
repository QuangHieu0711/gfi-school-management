package com.gfi.backend.models.dtos.common;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FileUploadDto {
    private String fileName;
    private String url;
    private long size;
    private String contentType;
}
