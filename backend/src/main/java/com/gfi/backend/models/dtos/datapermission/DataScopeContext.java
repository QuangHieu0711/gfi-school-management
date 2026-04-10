package com.gfi.backend.models.dtos.datapermission;

import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DataScopeContext {
    private List<String> scopeTypes;
    private Long userId;
}
