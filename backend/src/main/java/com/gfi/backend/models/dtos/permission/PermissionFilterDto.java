package com.gfi.backend.models.dtos.permission;

import lombok.Data;

@Data
public class PermissionFilterDto {
    private Long menuId;
    private Long roleId;
    private String menuKeyword;
    private String roleKeyword;
}
