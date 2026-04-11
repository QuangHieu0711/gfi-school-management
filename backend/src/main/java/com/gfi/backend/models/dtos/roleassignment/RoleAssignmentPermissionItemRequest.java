package com.gfi.backend.models.dtos.roleassignment;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RoleAssignmentPermissionItemRequest {

    @NotNull(message = "targetRoleId không được để trống")
    private Long targetRoleId;

    private Integer canCreate = 0;

    private Integer canUpdate = 0;
}
