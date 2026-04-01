package com.gfi.backend.models.dtos.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserCreateRequest {
    @NotBlank(message = "Ten dang nhap khong duoc de trong")
    @Size(max = 255, message = "Ten dang nhap toi da 255 ky tu")
    private String username;

    @NotBlank(message = "Mat khau khong duoc de trong")
    @Size(min = 6, max = 255, message = "Mat khau phai tu 6 den 255 ky tu")
    private String password;

    @NotBlank(message = "Ho ten khong duoc de trong")
    @Size(max = 255, message = "Ho ten toi da 255 ky tu")
    private String fullName;

    @Email(message = "Email khong dung dinh dang")
    @Size(max = 100, message = "Email toi da 100 ky tu")
    private String email;

    @NotNull(message = "Role khong duoc de trong")
    private Long roleId;

    @NotNull(message = "Don vi khong duoc de trong")
    private Long unitId;

    private Integer status;
}
