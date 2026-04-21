package com.gfi.backend.models.dtos.subject;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SubjectImportResultDto {
    private int successCount;
    private int failedCount;
    private boolean hasErrorFile;
    private String errorFileToken;
    private String errorFileName;
}
