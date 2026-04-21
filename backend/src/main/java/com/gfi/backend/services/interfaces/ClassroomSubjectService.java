package com.gfi.backend.services.interfaces;

import com.gfi.backend.models.dtos.classroomsubject.ClassroomSubjectAssignRequest;
import com.gfi.backend.models.dtos.classroomsubject.ClassroomSubjectConfigDto;
import com.gfi.backend.models.dtos.common.LookupItemDto;
import com.gfi.backend.models.entities.Classroom;

import java.util.List;

public interface ClassroomSubjectService {
    ClassroomSubjectConfigDto getByClassroomId(Long classroomId);
    ClassroomSubjectConfigDto assignSubjects(ClassroomSubjectAssignRequest request);
    List<LookupItemDto> getClassroomsBySubjectId(Long subjectId, Long unitId);
    void syncFromGradeLevel(Classroom classroom);
    void clearByClassroomId(Long classroomId);
}
