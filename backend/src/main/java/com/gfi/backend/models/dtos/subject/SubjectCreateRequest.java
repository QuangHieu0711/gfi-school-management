package com.gfi.backend.models.dtos.subject;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SubjectCreateRequest {
    @NotBlank(message = "Ma mon hoc khong duoc de trong")
    @Size(max = 50, message = "Ma mon hoc toi da 50 ky tu")
    private String code;

    @NotBlank(message = "Ten mon hoc khong duoc de trong")
    @Size(max = 255, message = "Ten mon hoc toi da 255 ky tu")
    private String name;

    @NotNull(message = "Loai mon hoc khong duoc de trong")
    private Integer type;

    @Size(max = 500, message = "Ghi chu toi da 500 ky tu")
    private String description;

    @NotNull(message = "Trang thai khong duoc de trong")
    private Integer status;
}
