package com.gfi.backend.models.dtos.staff;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TeacherAssignmentImportResultDto {
    private int successCount;
    private int failedCount;
}
