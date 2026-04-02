package com.gfi.backend.services.interfaces;

import com.gfi.backend.models.dtos.gradelevelsubject.GradeLevelSubjectAssignRequest;
import com.gfi.backend.models.dtos.gradelevelsubject.GradeLevelSubjectConfigDto;

public interface GradeLevelSubjectService {
    GradeLevelSubjectConfigDto getByGradeLevelId(Long gradeLevelId);
    GradeLevelSubjectConfigDto assignSubjects(GradeLevelSubjectAssignRequest request);
}
