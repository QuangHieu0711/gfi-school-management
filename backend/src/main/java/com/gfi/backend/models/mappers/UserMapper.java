package com.gfi.backend.models.mappers;

import com.gfi.backend.models.dtos.user.UserDetailDto;
import com.gfi.backend.models.dtos.user.UserListItemDto;
import com.gfi.backend.models.entities.User;
import org.springframework.stereotype.Component;

/**
 * Mapper chuyển User entity sang các DTO.
 * Tách biệt mapping logic khỏi service layer.
 */
@Component
public class UserMapper {

    /**
     * Chuyển User entity sang UserListItemDto (cho list/search).
     * Chỉ chứa những field cần thiết để tránh over-fetching.
     */
    public UserListItemDto toListItemDto(User user) {
        return UserListItemDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .status(user.getStatus())
                .roleName(user.getRole() == null ? null : user.getRole().getRoleName())
                .unitName(user.getUnit() == null ? null : user.getUnit().getName())
                .build();
    }

    /**
     * Chuyển User entity sang UserDetailDto (cho detail/create/update).
     * Chứa đầy đủ thông tin bao gồm quan hệ.
     */
    public UserDetailDto toDetailDto(User user) {
        return UserDetailDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .status(user.getStatus())
                .roleId(user.getRole() == null ? null : user.getRole().getId())
                .unitId(user.getUnit() == null ? null : user.getUnit().getId())
                .build();
    }
}
