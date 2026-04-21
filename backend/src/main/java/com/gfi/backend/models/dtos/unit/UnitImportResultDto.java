package com.gfi.backend.models.dtos.unit;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UnitImportResultDto {
    private int successCount;
    private int failedCount;
}
