package com.gfi.backend.models.dtos.datapermission;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DataPermissionScopeItemDto {
    private Long id;
    private String scopeType;
    private Integer status;
}
