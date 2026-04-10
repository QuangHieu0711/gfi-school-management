package com.gfi.backend.models.dtos.user;

import lombok.Builder;
import lombok.Data;

/**
 * DTO cho API detail/create/update users.
 * Chứa đầy đủ thông tin user bao gồm quan hệ.
 */
@Data
@Builder
public class UserDetailDto {
    private Long id;
    private String username;
    private String fullName;
    private String email;
    private String phone;
    private Integer status;
    private Long roleId;
    private Long unitId;
}
