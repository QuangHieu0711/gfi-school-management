package com.gfi.backend.models.dtos.datapermission;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DataPermissionSaveRequest {
    @NotNull(message = "Menu không được để trống")
    private Long menuId;

    @NotNull(message = "Role không được để trống")
    private Long roleId;

    @NotNull(message = "Trạng thái không được để trống")
    private Integer status;

    @NotNull(message = "Phạm vi không được để trống")
    private List<@Valid DataPermissionScopeSaveRequest> scopes;
}
