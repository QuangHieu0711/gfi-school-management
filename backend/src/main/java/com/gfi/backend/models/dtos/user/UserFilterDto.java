package com.gfi.backend.models.dtos.user;

import lombok.Data;

@Data
public class UserFilterDto {
    private String fullName;
    private Long roleId;
    private Long unitId;
    private Integer status;
}
