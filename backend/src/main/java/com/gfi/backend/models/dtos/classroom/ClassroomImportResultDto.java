package com.gfi.backend.models.dtos.classroom;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ClassroomImportResultDto {
    private int successCount;
    private int failedCount;
    private boolean hasErrorFile;
    private String errorFileToken;
    private String errorFileName;
}
