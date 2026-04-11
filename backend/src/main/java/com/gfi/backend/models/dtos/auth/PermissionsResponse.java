package com.gfi.backend.models.dtos.auth;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PermissionsResponse {
    private List<MenuPermissionDto> menus;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MenuPermissionDto {
        private String menuCode;
        private String menuName;
        private String path;
        private String icon;
        private Integer level;
        private Long parentMenuId;
        private ActionDto actions;
        private List<DataScopeDto> dataScopes;
        private List<MenuPermissionDto> children;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActionDto {
        private Integer isView;
        private Integer isAdd;
        private Integer isEdit;
        private Integer isDelete;
        private Integer isDownload;
        private Integer isConfig;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DataScopeDto {
        private String scopeType;
        private List<Long> scopeValues;
    }
}
