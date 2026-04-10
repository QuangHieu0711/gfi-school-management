package com.gfi.backend.models.dtos.subject;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SubjectUpdateRequest {
    @NotBlank(message = "Mã môn học không được để trống")
    @Size(max = 50, message = "Mã môn học tối đa 50 ký tự")
    private String code;

    @NotBlank(message = "Tên môn học không được để trống")
    @Size(max = 255, message = "Tên môn học tối đa 255 ký tự")
    private String name;

    @NotNull(message = "Loại môn học không được để trống")
    private Integer type;

    @Size(max = 500, message = "Ghi chú tối đa 500 ký tự")
    private String description;

    @NotNull(message = "Trạng thái không được để trống")
    private Integer status;
}
