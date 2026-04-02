package com.gfi.backend.models.dtos.role;

import lombok.Data;

@Data
public class RoleFilterDto {
    private String code;
    private String roleName;
    private Integer status;
}
