package com.gfi.backend.services.interfaces;

import java.util.List;

import com.gfi.backend.models.dtos.common.LookupItemDto;
import com.gfi.backend.models.dtos.semester.SemesterCreateRequest;
import com.gfi.backend.models.dtos.semester.SemesterFilterDto;
import com.gfi.backend.models.dtos.semester.SemesterItemDto;
import com.gfi.backend.models.dtos.semester.SemesterUpdateRequest;

public interface SemesterService {
    List<SemesterItemDto> search(SemesterFilterDto filter);
    List<LookupItemDto> getOptions(Long schoolYearId);
    SemesterItemDto getById(Long id);
    SemesterItemDto create(SemesterCreateRequest request);
    SemesterItemDto update(Long id, SemesterUpdateRequest request);
    void delete(Long id);
}
