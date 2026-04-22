package com.gfi.backend.models.dtos.staff;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StaffImportResultDto {
    private int successCount;
    private int failedCount;
    private boolean hasErrorFile;
    private String errorFileToken;
    private String errorFileName;
}
