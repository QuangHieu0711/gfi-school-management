package com.gfi.backend.models.dtos.permission;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PermissionItemDto {
    private Long id;
    private Long menuId;
    private String menuCode;
    private String menuName;
    private Long roleId;
    private String roleCode;
    private String roleName;
    private Integer isAdd;
    private Integer isApprove;
    private Integer isDelete;
    private Integer isDownload;
    private Integer isEdit;
    private Integer isView;
}
