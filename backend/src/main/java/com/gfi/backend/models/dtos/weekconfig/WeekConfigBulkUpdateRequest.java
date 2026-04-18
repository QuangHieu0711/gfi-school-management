package com.gfi.backend.models.dtos.weekconfig;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class WeekConfigBulkUpdateRequest {
    @Valid
    @NotEmpty(message = "Danh sách cấu hình tuần học không được để trống")
    private List<WeekConfigBulkUpdateItemRequest> items;
}
