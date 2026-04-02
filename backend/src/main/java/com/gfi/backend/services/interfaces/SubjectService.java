package com.gfi.backend.services.interfaces;

import java.util.List;

import com.gfi.backend.models.dtos.common.LookupItemDto;
import com.gfi.backend.models.dtos.common.PageRequestDto;
import com.gfi.backend.models.dtos.common.PageResponseDto;
import com.gfi.backend.models.dtos.subject.SubjectCreateRequest;
import com.gfi.backend.models.dtos.subject.SubjectFilterDto;
import com.gfi.backend.models.dtos.subject.SubjectItemDto;
import com.gfi.backend.models.dtos.subject.SubjectUpdateRequest;

public interface SubjectService {
    PageResponseDto<SubjectItemDto, SubjectFilterDto> search(PageRequestDto<SubjectFilterDto> request);
    List<LookupItemDto> getOptions();
    SubjectItemDto getById(Long id);
    SubjectItemDto create(SubjectCreateRequest request);
    SubjectItemDto update(Long id, SubjectUpdateRequest request);
    void delete(Long id);
}
