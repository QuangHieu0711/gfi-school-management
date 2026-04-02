package com.gfi.backend.models.dtos.gradelevel;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class GradeLevelCreateRequest {
    @NotBlank(message = "Mã khối không được để trống")
    @Size(max = 50, message = "Mã khối tối đa 50 ký tự")
    private String code;

    @NotBlank(message = "Tên khối không được để trống")
    @Size(max = 255, message = "Tên khối tối đa 255 ký tự")
    private String name;

    @NotNull(message = "Số khối không được để trống")
    @Min(value = 1, message = "Số khối phải lớn hơn 0")
    private Integer gradeNumber;

    @NotNull(message = "Trạng thái không được để trống")
    private Integer status;

    @Size(max = 500, message = "Ghi chú tối đa 500 ký tự")
    private String description;
}
