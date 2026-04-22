package com.gfi.backend.models.dtos.student;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StudentImportResultDto {
    private int successCount;
    private int failedCount;
    private boolean hasErrorFile;
    private String errorFileToken;
    private String errorFileName;
}
