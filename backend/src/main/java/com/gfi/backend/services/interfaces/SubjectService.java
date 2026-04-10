package com.gfi.backend.services.interfaces;

import java.util.List;

import com.gfi.backend.models.dtos.common.LookupItemDto;
import com.gfi.backend.models.dtos.common.PageRequestDto;
import com.gfi.backend.models.dtos.common.PageResponseDto;
import com.gfi.backend.models.dtos.subject.SubjectCreateRequest;
import com.gfi.backend.models.dtos.subject.SubjectDetailDto;
import com.gfi.backend.models.dtos.subject.SubjectFilterDto;
import com.gfi.backend.models.dtos.subject.SubjectListItemDto;
import com.gfi.backend.models.dtos.subject.SubjectUpdateRequest;

public interface SubjectService {
    PageResponseDto<SubjectListItemDto, SubjectFilterDto> search(PageRequestDto<SubjectFilterDto> request);
    List<LookupItemDto> getOptions();
    SubjectDetailDto getById(Long id);
    SubjectDetailDto create(SubjectCreateRequest request);
    SubjectDetailDto update(Long id, SubjectUpdateRequest request);
    void delete(Long id);
}
