package com.gfi.backend.models.dtos.programdistribution;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProgramDistributionImportResultDto {
    private long successCount;
    private long failedCount;
}
