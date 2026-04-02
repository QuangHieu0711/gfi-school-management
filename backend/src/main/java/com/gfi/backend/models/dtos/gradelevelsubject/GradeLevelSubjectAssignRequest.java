package com.gfi.backend.models.dtos.gradelevelsubject;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class GradeLevelSubjectAssignRequest {
    @NotNull(message = "Khoi khong duoc de trong")
    private Long gradeLevelId;

    @NotEmpty(message = "Danh sach mon hoc khong duoc de trong")
    private List<Long> subjectIds;
}
