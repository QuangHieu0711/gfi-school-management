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
                .unitName(user.getStaff() == null || user.getStaff().getUnit() == null ? null : user.getStaff().getUnit().getName())
                .email(user.getEmail() == null ? null : user.getEmail())
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
                .unitId(user.getUnitId())
                .staffId(user.getStaff() != null ? user.getStaff().getId() : null)
                .staffCode(user.getStaff() != null ? user.getStaff().getStaffCode() : null)
                .unitName(user.getStaff() != null && user.getStaff().getUnit() != null ? user.getStaff().getUnit().getName() : null)
                .build();
    }
}
