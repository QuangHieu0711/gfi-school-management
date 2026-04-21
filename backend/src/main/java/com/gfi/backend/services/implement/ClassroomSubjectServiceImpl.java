package com.gfi.backend.services.implement;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gfi.backend.controllers.exceptions.UserMessageException;
import com.gfi.backend.models.dtos.classroomsubject.ClassroomSubjectAssignRequest;
import com.gfi.backend.models.dtos.classroomsubject.ClassroomSubjectConfigDto;
import com.gfi.backend.models.dtos.classroomsubject.ClassroomSubjectItemDto;
import com.gfi.backend.models.dtos.common.LookupItemDto;
import com.gfi.backend.models.entities.Classroom;
import com.gfi.backend.models.entities.ClassroomSubject;
import com.gfi.backend.models.entities.GradeLevelSubject;
import com.gfi.backend.models.entities.Subject;
import com.gfi.backend.models.global.CommonErrorCode;
import com.gfi.backend.repositories.ClassroomRepository;
import com.gfi.backend.repositories.ClassroomSubjectRepository;
import com.gfi.backend.repositories.GradeLevelSubjectRepository;
import com.gfi.backend.repositories.SubjectRepository;
import com.gfi.backend.services.interfaces.ClassroomSubjectService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ClassroomSubjectServiceImpl implements ClassroomSubjectService {

    private final ClassroomRepository classroomRepository;
    private final ClassroomSubjectRepository classroomSubjectRepository;
    private final GradeLevelSubjectRepository gradeLevelSubjectRepository;
    private final SubjectRepository subjectRepository;

    @Override
    public ClassroomSubjectConfigDto getByClassroomId(Long classroomId) {
        Classroom classroom = findClassroom(classroomId);
        List<GradeLevelSubject> inheritedSubjects = gradeLevelSubjectRepository.findByGradeLevelId(classroom.getGradeLevel().getId());
        List<ClassroomSubject> classroomSubjects = classroomSubjectRepository.findByClassroomId(classroomId);
        return toConfigDto(classroom, inheritedSubjects, classroomSubjects);
    }

    @Override
    @Transactional
    public ClassroomSubjectConfigDto assignSubjects(ClassroomSubjectAssignRequest request) {
        Classroom classroom = findClassroom(request.getClassroomId());
        List<GradeLevelSubject> inheritedSubjects = gradeLevelSubjectRepository.findByGradeLevelId(classroom.getGradeLevel().getId());
        if (inheritedSubjects.isEmpty()) {
            throw new UserMessageException(CommonErrorCode.BAD_REQUEST);
        }

        Set<Long> inheritedSubjectIds = inheritedSubjects.stream()
                .map(item -> item.getSubject().getId())
                .collect(LinkedHashSet::new, Set::add, Set::addAll);
        Set<Long> selectedSubjectIds = normalizeSubjectIds(request.getSubjectIds());

        if (!inheritedSubjectIds.containsAll(selectedSubjectIds)) {
            throw new UserMessageException(CommonErrorCode.BAD_REQUEST);
        }

        replaceClassroomSubjects(classroom, selectedSubjectIds);
        List<ClassroomSubject> classroomSubjects = classroomSubjectRepository.findByClassroomId(classroom.getId());
        return toConfigDto(classroom, inheritedSubjects, classroomSubjects);
    }

        @Override
        @Transactional(readOnly = true)
        public List<LookupItemDto> getClassroomsBySubjectId(Long subjectId, Long unitId) {
        subjectRepository.findById(subjectId)
            .orElseThrow(() -> new UserMessageException(CommonErrorCode.SUBJECT_NOT_FOUND));

        return classroomSubjectRepository.findActiveClassroomsBySubjectId(subjectId, unitId)
            .stream()
            .map(item -> LookupItemDto.builder()
                .id(item.getId())
                .name(item.getName())
                .build())
            .toList();
        }

    @Override
    @Transactional
    public void syncFromGradeLevel(Classroom classroom) {
        List<GradeLevelSubject> inheritedSubjects = gradeLevelSubjectRepository.findByGradeLevelId(classroom.getGradeLevel().getId());
        Set<Long> subjectIds = inheritedSubjects.stream()
                .map(item -> item.getSubject().getId())
                .collect(LinkedHashSet::new, Set::add, Set::addAll);
        replaceClassroomSubjects(classroom, subjectIds);
    }

    @Override
    @Transactional
    public void clearByClassroomId(Long classroomId) {
        classroomSubjectRepository.deleteByClassroomId(classroomId);
        classroomSubjectRepository.flush();
    }

    private void replaceClassroomSubjects(Classroom classroom, Set<Long> subjectIds) {
        String username = getCurrentUsername();
        classroomSubjectRepository.deleteByClassroomId(classroom.getId());
        classroomSubjectRepository.flush();

        List<ClassroomSubject> items = new ArrayList<>();
        for (Long subjectId : subjectIds) {
            Subject subject = new Subject();
            subject.setId(subjectId);

            ClassroomSubject classroomSubject = new ClassroomSubject();
            classroomSubject.setClassroom(classroom);
            classroomSubject.setSubject(subject);
            classroomSubject.setStatus(1);
            classroomSubject.setCreatedBy(username);
            items.add(classroomSubject);
        }

        if (!items.isEmpty()) {
            classroomSubjectRepository.saveAll(items);
        }
    }

    private ClassroomSubjectConfigDto toConfigDto(Classroom classroom, List<GradeLevelSubject> inheritedSubjects, List<ClassroomSubject> classroomSubjects) {
        Set<Long> selectedSubjectIds = classroomSubjects.stream()
                .map(item -> item.getSubject().getId())
                .collect(LinkedHashSet::new, Set::add, Set::addAll);

        List<GradeLevelSubject> sortedInheritedSubjects = inheritedSubjects.stream()
                .sorted((left, right) -> {
                    String leftName = left.getSubject().getName() == null ? "" : left.getSubject().getName();
                    String rightName = right.getSubject().getName() == null ? "" : right.getSubject().getName();
                    int compareByName = leftName.compareToIgnoreCase(rightName);
                    if (compareByName != 0) {
                        return compareByName;
                    }
                    return left.getSubject().getId().compareTo(right.getSubject().getId());
                })
                .toList();

        List<ClassroomSubjectItemDto> subjects = sortedInheritedSubjects.stream()
                .map(item -> ClassroomSubjectItemDto.builder()
                        .subjectId(item.getSubject().getId())
                        .subjectCode(item.getSubject().getCode())
                        .subjectName(item.getSubject().getName())
                        .subjectType(item.getSubject().getType())
                        .selected(selectedSubjectIds.contains(item.getSubject().getId()))
                        .build())
                .toList();

        return ClassroomSubjectConfigDto.builder()
                .classroomId(classroom.getId())
                .classroomName(classroom.getName())
                .gradeLevelId(classroom.getGradeLevel().getId())
                .gradeLevelName(classroom.getGradeLevel().getName())
                .subjects(subjects)
                .build();
    }

    private Classroom findClassroom(Long id) {
        return classroomRepository.findById(id)
                .orElseThrow(() -> new UserMessageException(CommonErrorCode.CLASS_NOT_FOUND));
    }

    private Set<Long> normalizeSubjectIds(List<Long> subjectIds) {
        Set<Long> normalized = new LinkedHashSet<>();
        if (subjectIds == null) {
            return normalized;
        }
        for (Long subjectId : subjectIds) {
            if (subjectId != null) {
                normalized.add(subjectId);
            }
        }
        return normalized;
    }

    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || "anonymousUser".equals(authentication.getName())) {
            return "SYSTEM";
        }
        return authentication.getName();
    }
}
