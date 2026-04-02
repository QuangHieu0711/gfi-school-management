package com.gfi.backend.models.dtos.role;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RoleCreateRequest {
    @NotBlank(message = "Ma role khong duoc de trong")
    @Size(max = 50, message = "Ma role toi da 50 ky tu")
    private String code;

    @NotBlank(message = "Ten role khong duoc de trong")
    @Size(max = 100, message = "Ten role toi da 100 ky tu")
    private String roleName;

    @Size(max = 255, message = "Mo ta toi da 255 ky tu")
    private String description;

    @NotNull(message = "Trang thai khong duoc de trong")
    @Min(value = 0, message = "Trang thai chi duoc la 0 hoac 1")
    @Max(value = 1, message = "Trang thai chi duoc la 0 hoac 1")
    private Integer status;
}
