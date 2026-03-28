package com.gfi.backend.models.dtos.user;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserItemDto {
    private Long id;
    private String username;
    private String fullName;
    private Long roleId;
    private String roleName;
    private Long unitId;
    private String unitCode;
    private String unitName;
    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime updatedAt;
    private String updatedBy;
}
