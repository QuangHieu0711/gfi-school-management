package com.gfi.backend.services.interfaces;

import java.util.List;

import com.gfi.backend.models.dtos.weekconfig.WeekConfigBulkUpdateRequest;
import com.gfi.backend.models.dtos.weekconfig.WeekConfigCbbDto;
import com.gfi.backend.models.dtos.weekconfig.WeekConfigGenerateRequest;
import com.gfi.backend.models.dtos.weekconfig.WeekConfigItemDto;
import com.gfi.backend.models.dtos.weekconfig.WeekConfigUpdateRequest;

public interface WeekConfigService {
    List<WeekConfigItemDto> getWeekConfigs(Long schoolYearId, Long semesterId);

    List<WeekConfigItemDto> generate(WeekConfigGenerateRequest request);

    WeekConfigItemDto update(Long id, WeekConfigUpdateRequest request);

    List<WeekConfigItemDto> bulkUpdate(WeekConfigBulkUpdateRequest request);

    void deleteBySemester(Long semesterId);

    List<WeekConfigCbbDto> getWeekConfigsCbb();
}
