package com.gfi.backend.models.dtos.role;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RoleUpdateRequest {
    @NotBlank(message = "Tên role không được để trống")
    @Size(max = 100, message = "Tên role tối đa 100 ký tự")
    private String roleName;

    @Size(max = 255, message = "Mô tả tối đa 255 ký tự")
    private String description;
}
