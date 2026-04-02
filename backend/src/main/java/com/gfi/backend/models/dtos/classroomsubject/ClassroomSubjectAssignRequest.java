package com.gfi.backend.models.dtos.classroomsubject;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ClassroomSubjectAssignRequest {
    @NotNull(message = "Lop khong duoc de trong")
    private Long classroomId;

    @NotEmpty(message = "Danh sach mon hoc khong duoc de trong")
    private List<Long> subjectIds;
}
