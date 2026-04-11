package com.gfi.backend.models.dtos.roleassignment;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RoleAssignmentPermissionSaveRequest {

    @NotNull(message = "creatorRoleId không được để trống")
    private Long creatorRoleId;

    @Valid
    private List<RoleAssignmentPermissionItemRequest> items = new ArrayList<>();
}
