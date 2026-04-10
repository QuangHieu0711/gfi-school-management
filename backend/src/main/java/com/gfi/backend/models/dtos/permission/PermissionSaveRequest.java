package com.gfi.backend.models.dtos.permission;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PermissionSaveRequest {
    @NotNull(message = "Menu không được để trống")
    private Long menuId;

    @NotNull(message = "Role không được để trống")
    private Long roleId;

    @NotNull(message = "isAdd không được để trống")
    @Min(value = 0, message = "Giá trị chỉ được là 0 hoặc 1")
    @Max(value = 1, message = "Giá trị chỉ được là 0 hoặc 1")
    private Integer isAdd;

    @NotNull(message = "isDelete không được để trống")
    @Min(value = 0, message = "Giá trị chỉ được là 0 hoặc 1")
    @Max(value = 1, message = "Giá trị chỉ được là 0 hoặc 1")
    private Integer isDelete;

    @NotNull(message = "isDownload không được để trống")
    @Min(value = 0, message = "Giá trị chỉ được là 0 hoặc 1")
    @Max(value = 1, message = "Giá trị chỉ được là 0 hoặc 1")
    private Integer isDownload;

    @NotNull(message = "isConfig không được để trống")
    @Min(value = 0, message = "Giá trị chỉ được là 0 hoặc 1")
    @Max(value = 1, message = "Giá trị chỉ được là 0 hoặc 1")
    private Integer isConfig;

    @NotNull(message = "isEdit không được để trống")
    @Min(value = 0, message = "Giá trị chỉ được là 0 hoặc 1")
    @Max(value = 1, message = "Giá trị chỉ được là 0 hoặc 1")
    private Integer isEdit;

    @NotNull(message = "isView không được để trống")
    @Min(value = 0, message = "Giá trị chỉ được là 0 hoặc 1")
    @Max(value = 1, message = "Giá trị chỉ được là 0 hoặc 1")
    private Integer isView;
}
