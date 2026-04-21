package com.gfi.backend.models.dtos.schoolyear;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SchoolYearImportResultDto {
    private int successCount;
    private int failedCount;
    private boolean hasErrorFile;
    private String errorFileToken;
    private String errorFileName;
}
