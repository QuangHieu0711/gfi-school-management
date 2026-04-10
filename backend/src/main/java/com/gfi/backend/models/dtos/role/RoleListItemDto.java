package com.gfi.backend.models.dtos.role;

import lombok.Builder;
import lombok.Data;

/**
 * DTO cho API list/search roles.
 * Chứa thông tin tối thiểu để tránh over-fetching.
 */
@Data
@Builder
public class RoleListItemDto {
    private Long id;
    private String code;
    private String roleName;
    private Integer status;
}
