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
import com.gfi.backend.models.dtos.gradelevelsubject.GradeLevelSubjectAssignRequest;
import com.gfi.backend.models.dtos.gradelevelsubject.GradeLevelSubjectConfigDto;
import com.gfi.backend.models.dtos.gradelevelsubject.GradeLevelSubjectItemDto;
import com.gfi.backend.models.entities.GradeLevel;
import com.gfi.backend.models.entities.GradeLevelSubject;
import com.gfi.backend.models.entities.Subject;
import com.gfi.backend.models.global.CommonErrorCode;
import com.gfi.backend.repositories.GradeLevelRepository;
import com.gfi.backend.repositories.GradeLevelSubjectRepository;
import com.gfi.backend.repositories.SubjectRepository;
import com.gfi.backend.services.interfaces.GradeLevelSubjectService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GradeLevelSubjectServiceImpl implements GradeLevelSubjectService {

    private final GradeLevelRepository gradeLevelRepository;
    private final SubjectRepository subjectRepository;
    private final GradeLevelSubjectRepository gradeLevelSubjectRepository;

    @Override
    public GradeLevelSubjectConfigDto getByGradeLevelId(Long gradeLevelId) {
        GradeLevel gradeLevel = findGradeLevel(gradeLevelId);
        List<GradeLevelSubject> mappings = gradeLevelSubjectRepository.findByGradeLevelId(gradeLevelId);
        return toConfigDto(gradeLevel, mappings);
    }

    @Override
    @Transactional
    public GradeLevelSubjectConfigDto assignSubjects(GradeLevelSubjectAssignRequest request) {
        GradeLevel gradeLevel = findGradeLevel(request.getGradeLevelId());
        Set<Long> subjectIds = normalizeSubjectIds(request.getSubjectIds());
        List<Subject> subjects = subjectRepository.findAllById(subjectIds);
        if (subjects.size() != subjectIds.size()) {
            throw new UserMessageException(CommonErrorCode.SUBJECT_NOT_FOUND);
        }

        String username = getCurrentUsername();

        gradeLevelSubjectRepository.deleteByGradeLevelId(gradeLevel.getId());
        gradeLevelSubjectRepository.flush();

        List<GradeLevelSubject> mappings = new ArrayList<>();
        for (Subject subject : subjects) {
            GradeLevelSubject item = new GradeLevelSubject();
            item.setGradeLevel(gradeLevel);
            item.setSubject(subject);
            item.setStatus(1);
            item.setCreatedBy(username);
            mappings.add(item);
        }

        List<GradeLevelSubject> savedItems = gradeLevelSubjectRepository.saveAll(mappings);
        return toConfigDto(gradeLevel, savedItems);
    }

    private GradeLevel findGradeLevel(Long id) {
        return gradeLevelRepository.findById(id)
                .orElseThrow(() -> new UserMessageException(CommonErrorCode.GRADE_LEVEL_NOT_FOUND));
    }

    private Set<Long> normalizeSubjectIds(List<Long> subjectIds) {
        Set<Long> normalized = new LinkedHashSet<>();
        for (Long subjectId : subjectIds) {
            if (subjectId != null) {
                normalized.add(subjectId);
            }
        }
        if (normalized.isEmpty()) {
            throw new UserMessageException(CommonErrorCode.BAD_REQUEST);
        }
        return normalized;
    }

    private GradeLevelSubjectConfigDto toConfigDto(GradeLevel gradeLevel, List<GradeLevelSubject> mappings) {
        List<GradeLevelSubject> sortedMappings = mappings.stream()
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

        List<Long> subjectIds = sortedMappings.stream()
                .map(item -> item.getSubject().getId())
                .toList();

        List<GradeLevelSubjectItemDto> subjects = sortedMappings.stream()
                .map(this::toItemDto)
                .toList();

        return GradeLevelSubjectConfigDto.builder()
                .gradeLevelId(gradeLevel.getId())
                .gradeLevelName(gradeLevel.getName())
                .subjectIds(subjectIds)
                .subjects(subjects)
                .build();
    }

    private GradeLevelSubjectItemDto toItemDto(GradeLevelSubject item) {
        return GradeLevelSubjectItemDto.builder()
                .id(item.getId())
                .subjectId(item.getSubject().getId())
                .subjectCode(item.getSubject().getCode())
                .subjectName(item.getSubject().getName())
                .subjectType(item.getSubject().getType())
                .build();
    }

    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || "anonymousUser".equals(authentication.getName())) {
            return "SYSTEM";
        }
        return authentication.getName();
    }
}
