package com.gfi.backend.models.dtos.datapermission;

import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DataPermissionItemDto {
    private Long id;
    private Long roleId;
    private String roleCode;
    private String roleName;
    private Long menuId;
    private Long parentId;
    private String menuCode;
    private String menuName;
    private String menuUrl;
    private String icon;
    private Integer ordinal;
    private Integer status;
    private List<DataPermissionScopeItemDto> scopes;
}
