package com.gfi.backend.models.dtos.role;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RoleCreateRequest {
    @NotBlank(message = "Tên role không được để trống")
    @Size(max = 100, message = "Tên role tối đa 100 ký tự")
    private String roleName;

    @Size(max = 255, message = "Mô tả tối đa 255 ký tự")
    private String description;

    @NotNull(message = "Trạng thái không được để trống")
    @Min(value = 0, message = "Trạng thái chỉ được là 0 hoặc 1")
    @Max(value = 1, message = "Trạng thái chỉ được là 0 hoặc 1")
    private Integer status;
}
