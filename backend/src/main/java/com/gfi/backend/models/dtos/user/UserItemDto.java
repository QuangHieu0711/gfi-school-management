package com.gfi.backend.models.dtos.user;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserItemDto {
    private Long id;
    private String username;
    private String fullName;
    private String email;
    private Integer status;
    private Long roleId;
    private String roleName;
    private Long unitId;
    private String unitName;
}
