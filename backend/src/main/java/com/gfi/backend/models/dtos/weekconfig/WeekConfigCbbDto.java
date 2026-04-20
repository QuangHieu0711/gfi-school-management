package com.gfi.backend.models.dtos.weekconfig;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeekConfigCbbDto {
    private Long id;
    private String name;
}
