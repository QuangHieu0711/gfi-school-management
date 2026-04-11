package com.gfi.backend.models.dtos.roleassignment;

import java.util.ArrayList;
import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RoleAssignmentPermissionResponse {
    private Long creatorRoleId;
    private String creatorRoleCode;
    private String creatorRoleName;

    @Builder.Default
    private List<RoleAssignmentPermissionItemDto> items = new ArrayList<>();
}
