package com.gfi.backend.models.dtos.menu;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MenuUpdateRequest {
    private Long parentId;

    @NotBlank(message = "Ma menu khong duoc de trong")
    @Size(max = 100, message = "Ma menu toi da 100 ky tu")
    private String code;

    @NotBlank(message = "Ten menu khong duoc de trong")
    @Size(max = 255, message = "Ten menu toi da 255 ky tu")
    private String name;

    @Size(max = 255, message = "Icon toi da 255 ky tu")
    private String icon;

    @Size(max = 500, message = "Url toi da 500 ky tu")
    private String url;

    @NotNull(message = "Thu tu khong duoc de trong")
    private Integer ordinal;
}
