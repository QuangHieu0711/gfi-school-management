package com.gfi.backend.models.dtos.user;

import lombok.Builder;
import lombok.Data;

/**
 * DTO cho API list/search users.
 * Chỉ chứa những thông tin tối thiểu để tránh over-fetching.
 */
@Data
@Builder
public class UserListItemDto {
    private Long id;
    private String username;
    private String fullName;
    private Integer status;
    private String roleName;
    private String unitName;
    private String email;
}
