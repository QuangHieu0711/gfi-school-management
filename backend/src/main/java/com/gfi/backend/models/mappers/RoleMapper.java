package com.gfi.backend.models.mappers;

import org.springframework.stereotype.Component;

import com.gfi.backend.models.dtos.role.RoleDetailDto;
import com.gfi.backend.models.dtos.role.RoleListItemDto;
import com.gfi.backend.models.entities.Role;

/**
 * Mapper chuyển đổi Role entity thành DTO.
 * Tách biệt logic mapping từ service.
 */
@Component
public class RoleMapper {

    /**
     * Chuyển Role entity thành DTO danh sách (thông tin tối thiểu).
     */
    public RoleListItemDto toListItemDto(Role role) {
        return RoleListItemDto.builder()
                .id(role.getId())
                .code(role.getCode())
                .roleName(role.getRoleName())
                .status(role.getStatus())
                .build();
    }

    /**
     * Chuyển Role entity thành DTO chi tiết (toàn bộ thông tin).
     */
    public RoleDetailDto toDetailDto(Role role) {
        return RoleDetailDto.builder()
                .id(role.getId())
                .code(role.getCode())
                .roleName(role.getRoleName())
                .description(role.getDescription())
                .status(role.getStatus())
                .build();
    }
}
