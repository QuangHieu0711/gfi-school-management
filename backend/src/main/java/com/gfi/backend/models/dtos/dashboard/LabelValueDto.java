package com.gfi.backend.models.dtos.dashboard;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO đơn giản dùng cho biểu đồ: nhãn + giá trị số.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class LabelValueDto {
    private String label;
    private long value;
}
