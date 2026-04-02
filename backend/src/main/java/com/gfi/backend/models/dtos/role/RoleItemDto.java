package com.gfi.backend.models.dtos.role;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RoleItemDto {
    private Long id;
    private String code;
    private String roleName;
    private String description;
    private Integer status;
}
