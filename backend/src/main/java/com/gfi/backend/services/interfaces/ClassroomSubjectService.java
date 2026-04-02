package com.gfi.backend.services.interfaces;

import com.gfi.backend.models.dtos.classroomsubject.ClassroomSubjectAssignRequest;
import com.gfi.backend.models.dtos.classroomsubject.ClassroomSubjectConfigDto;
import com.gfi.backend.models.entities.Classroom;

public interface ClassroomSubjectService {
    ClassroomSubjectConfigDto getByClassroomId(Long classroomId);
    ClassroomSubjectConfigDto assignSubjects(ClassroomSubjectAssignRequest request);
    void syncFromGradeLevel(Classroom classroom);
    void clearByClassroomId(Long classroomId);
}
