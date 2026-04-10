package com.gfi.backend.models.dtos.role;

import lombok.Builder;
import lombok.Data;

/**
 * DTO cho API detail/create/update roles.
 * Chứa toàn bộ thông tin vai trò.
 */
@Data
@Builder
public class RoleDetailDto {
    private Long id;
    private String code;
    private String roleName;
    private String description;
    private Integer status;
}
