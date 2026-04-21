package com.gfi.backend.models.dtos.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserUpdateRequest {
    @NotBlank(message = "Tên đăng nhập không được để trống")
    @Size(max = 255, message = "Tên đăng nhập tối đa 255 ký tự")
    private String username;

    @Size(min = 6, max = 255, message = "Mật khẩu phải từ 6 đến 255 ký tự")
    private String password;

    @NotNull(message = "Vai trò không được để trống")
    private Long roleId;

    private Long unitId;

    private Integer status;
}
