package com.gfi.backend.models.dtos.datapermission;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DataPermissionScopeSaveRequest {
    @NotBlank(message = "Phạm vi không được để trống")
    private String scopeType;

    @NotNull(message = "Trạng thái không được để trống")
    private Integer status;
}
