package com.gfi.backend.models.dtos.classroom;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ClassroomCreateRequest {
    @NotBlank(message = "Ma lop khong duoc de trong")
    @Size(max = 50, message = "Ma lop toi da 50 ky tu")
    private String code;

    @NotBlank(message = "Ten lop khong duoc de trong")
    @Size(max = 255, message = "Ten lop toi da 255 ky tu")
    private String name;

    @NotNull(message = "Don vi khong duoc de trong")
    private Long unitId;

    @NotNull(message = "Khoi khong duoc de trong")
    private Long gradeLevelId;

    @NotNull(message = "Nam hoc khong duoc de trong")
    private Long schoolYearId;

    @NotNull(message = "Trang thai khong duoc de trong")
    private Integer status;

    @Size(max = 500, message = "Ghi chu toi da 500 ky tu")
    private String description;
}
