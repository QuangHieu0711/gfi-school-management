package com.gfi.backend.models.dtos.unit;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UnitCreateRequest {
    @NotBlank(message = "Mã đơn vị không được để trống")
    @Size(max = 100, message = "Mã đơn vị tối đa 100 ký tự")
    private String code;

    @NotBlank(message = "Tên đơn vị không được để trống")
    @Size(max = 255, message = "Tên đơn vị tối đa 255 ký tự")
    private String name;

    @Size(max = 255, message = "Địa chỉ tối đa 255 ký tự")
    private String address;

    @Size(max = 50, message = "Số điện thoại tối đa 50 ký tự")
    private String phone;

    @Size(max = 100, message = "Email tối đa 100 ký tự")
    private String email;

    private Integer status;
}
