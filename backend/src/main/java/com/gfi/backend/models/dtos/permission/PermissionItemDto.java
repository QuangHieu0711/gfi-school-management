package com.gfi.backend.models.dtos.permission;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PermissionItemDto {
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
    private Integer isView;
    private Integer isAdd;
    private Integer isEdit;
    private Integer isDelete;
    private Integer isDownload;
}
