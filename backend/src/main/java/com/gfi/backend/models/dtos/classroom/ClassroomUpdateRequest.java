package com.gfi.backend.models.dtos.classroom;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ClassroomUpdateRequest {
    @NotBlank(message = "Mã lớp không được để trống")
    @Size(max = 50, message = "Mã lớp tối đa 50 ký tự")
    private String code;

    @NotBlank(message = "Tên lớp không được để trống")
    @Size(max = 255, message = "Tên lớp tối đa 255 ký tự")
    private String name;

    @NotNull(message = "Đơn vị không được để trống")
    private Long unitId;

    @NotNull(message = "Khối không được để trống")
    private Long gradeLevelId;

    @NotNull(message = "Năm học không được để trống")
    private Long schoolYearId;

    @NotNull(message = "Trạng thái không được để trống")
    private Integer status;

    @Size(max = 500, message = "Ghi chú tối đa 500 ký tự")
    private String description;
}
