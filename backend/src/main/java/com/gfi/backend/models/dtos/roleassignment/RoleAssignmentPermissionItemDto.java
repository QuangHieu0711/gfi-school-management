package com.gfi.backend.models.dtos.roleassignment;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RoleAssignmentPermissionItemDto {
    private Long targetRoleId;
    private String targetRoleCode;
    private String targetRoleName;
    private Integer canCreate;
    private Integer canUpdate;
}
