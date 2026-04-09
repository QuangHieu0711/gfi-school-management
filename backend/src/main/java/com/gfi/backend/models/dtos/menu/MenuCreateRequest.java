package com.gfi.backend.models.dtos.menu;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MenuCreateRequest {
    private Long parentId;

    @NotBlank(message = "Mã menu không được để trống")
    @Size(max = 100, message = "Mã menu tối đa 100 ký tự")
    private String code;

    @NotBlank(message = "Tên menu không được để trống")
    @Size(max = 255, message = "Tên menu tối đa 255 ký tự")
    private String name;

    @Size(max = 500, message = "Url tối đa 500 ký tự")
    private String url;

    @Size(max = 255, message = "Icon tối đa 255 ký tự")
    private String icon;
}
