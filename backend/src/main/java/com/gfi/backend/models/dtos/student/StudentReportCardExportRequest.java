package com.gfi.backend.models.dtos.student;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class StudentReportCardExportRequest {

    @NotEmpty(message = "Danh sach hoc sinh khong duoc de trong")
    private List<Long> studentIds;
}
