package com.gfi.backend.services.implement;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gfi.backend.controllers.exceptions.UserMessageException;
import com.gfi.backend.models.dtos.classroom.ClassroomCreateRequest;
import com.gfi.backend.models.dtos.classroom.ClassroomDetailDto;
import com.gfi.backend.models.dtos.classroom.ClassroomFilterDto;
import com.gfi.backend.models.dtos.classroom.ClassroomGroupItemDto;
import com.gfi.backend.models.dtos.classroom.GradeLevelClassroomGroupDto;
import com.gfi.backend.models.dtos.classroom.ClassroomListItemDto;
import com.gfi.backend.models.dtos.classroom.ClassroomUpdateRequest;
import com.gfi.backend.models.dtos.common.LookupItemDto;
import com.gfi.backend.models.dtos.common.PageRequestDto;
import com.gfi.backend.models.dtos.common.PageResponseDto;
import com.gfi.backend.models.entities.Classroom;
import com.gfi.backend.models.entities.GradeLevel;
import com.gfi.backend.models.entities.SchoolYear;
import com.gfi.backend.models.entities.Unit;
import com.gfi.backend.models.enums.ActionType;
import com.gfi.backend.models.enums.ScopeType;
import com.gfi.backend.models.global.CommonErrorCode;
import com.gfi.backend.models.security.FeatureKey;
import com.gfi.backend.models.security.ResolvedScope;
import com.gfi.backend.repositories.ClassroomRepository;
import com.gfi.backend.repositories.GradeLevelRepository;
import com.gfi.backend.repositories.SchoolYearRepository;
import com.gfi.backend.repositories.UnitRepository;
import com.gfi.backend.repositories.specifications.ClassroomSpecification;
import com.gfi.backend.services.interfaces.ClassroomService;
import com.gfi.backend.services.interfaces.ClassroomSubjectService;
import com.gfi.backend.services.interfaces.DataScopeFilterService;
import com.gfi.backend.utils.PageableUtils;
import com.gfi.backend.utils.SecurityUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ClassroomServiceImpl implements ClassroomService {

    private final ClassroomRepository classroomRepository;
    private final UnitRepository unitRepository;
    private final GradeLevelRepository gradeLevelRepository;
    private final SchoolYearRepository schoolYearRepository;
    private final ClassroomSubjectService classroomSubjectService;
    private final ClassroomSpecification classroomSpecification;
    private final DataScopeFilterService dataScopeFilterService;

    private static final String FEATURE = FeatureKey.CLASS_MANAGEMENT.getCode();

    @Override
    @Transactional(readOnly = true)
    public PageResponseDto<ClassroomListItemDto, ClassroomFilterDto> search(
            PageRequestDto<ClassroomFilterDto> request) {
        ClassroomFilterDto filter = request.getFilter() == null ? new ClassroomFilterDto() : request.getFilter();
        int pageSize = normalizePageSize(request.getPageSize());
        int pageNow = normalizePageNow(request.getPageNow());
        Pageable pageable = PageableUtils.newestFirst(pageNow, pageSize);

        List<ResolvedScope> resolvedScopes = dataScopeFilterService.getResolvedScopes(FEATURE, ActionType.VIEW);
        Page<Classroom> classrooms = classroomRepository.findAll(
                classroomSpecification.buildSpecification(filter, resolvedScopes),
                pageable);
        Page<ClassroomListItemDto> page = classrooms.map(this::toListItemDto);

        return PageResponseDto.<ClassroomListItemDto, ClassroomFilterDto>builder()
                .pageSize(pageSize)
                .pageNow(pageNow)
                .filter(filter)
                .pageTotal(page.getTotalPages())
                .recordTotal(page.getTotalElements())
                .items(page.getContent())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<LookupItemDto> getOptions(Long unitId, Long gradeLevelId, Long schoolYearId) {
        List<ResolvedScope> resolvedScopes = dataScopeFilterService.getResolvedScopes(FEATURE, ActionType.VIEW);
        return classroomRepository.findAll(
                classroomSpecification.buildSpecificationForOptions(unitId, gradeLevelId, schoolYearId, resolvedScopes))
                .stream()
                .map(item -> LookupItemDto.builder().id(item.getId()).name(item.getName()).build())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<GradeLevelClassroomGroupDto> getGradeClassGroups(Long unitId, Long schoolYearId) {
        findUnit(unitId);
        findSchoolYear(schoolYearId);
        List<ResolvedScope> resolvedScopes = dataScopeFilterService.getResolvedScopes(FEATURE, ActionType.VIEW);

        return classroomRepository.findByUnitIdAndSchoolYearIdAndDeletedFlagOrderByGradeLevelGradeNumberAscNameAsc(
                unitId, schoolYearId, 0)
                .stream()
                .filter(classroom -> hasClassroomAccess(resolvedScopes, classroom))
                .filter(classroom -> classroom.getGradeLevel() != null)
                .collect(Collectors.groupingBy(
                        classroom -> classroom.getGradeLevel().getId(),
                        java.util.LinkedHashMap::new,
                        Collectors.toList()))
                .values()
                .stream()
                .map(group -> {
                    GradeLevel gradeLevel = group.get(0).getGradeLevel();
                    return GradeLevelClassroomGroupDto.builder()
                            .gradeLevelId(gradeLevel.getId())
                            .gradeLevelName(gradeLevel.getName())
                            .gradeNumber(gradeLevel.getGradeNumber())
                            .classes(group.stream()
                                    .map(item -> ClassroomGroupItemDto.builder()
                                            .id(item.getId())
                                            .name(item.getName())
                                            .build())
                                    .toList())
                            .build();
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ClassroomDetailDto getById(Long id) {
        Classroom classroom = findClassroom(id);
        validateClassroomScope(ActionType.VIEW, classroom);
        return toDetailDto(classroom);
    }

    @Override
    @Transactional
    public ClassroomDetailDto create(ClassroomCreateRequest request) {
        Unit unit = findUnit(request.getUnitId());
        GradeLevel gradeLevel = findGradeLevel(request.getGradeLevelId());
        validateClassroomTargetScope(ActionType.ADD, unit, gradeLevel);
        SchoolYear schoolYear = findSchoolYear(request.getSchoolYearId());
        String code = normalize(request.getCode());
        String name = normalize(request.getName());

        validateUnique(unit.getId(), gradeLevel.getId(), schoolYear.getId(), code, name, null);

        Classroom classroom = new Classroom();
        classroom.setCode(code);
        classroom.setName(name);
        classroom.setUnit(unit);
        classroom.setGradeLevel(gradeLevel);
        classroom.setSchoolYear(schoolYear);
        classroom.setStatus(request.getStatus());
        classroom.setDescription(normalizeNullable(request.getDescription()));
        classroom.setCreatedBy(SecurityUtils.getCurrentUsername());
        Classroom savedClassroom = classroomRepository.save(classroom);
        classroomSubjectService.syncFromGradeLevel(savedClassroom);
        return toDetailDto(savedClassroom);
    }

    @Override
    @Transactional
    public ClassroomDetailDto update(Long id, ClassroomUpdateRequest request) {
        Classroom classroom = findClassroom(id);
        validateClassroomScope(ActionType.EDIT, classroom);

        Unit unit = findUnit(request.getUnitId());
        GradeLevel gradeLevel = findGradeLevel(request.getGradeLevelId());
        validateClassroomTargetScope(ActionType.EDIT, unit, gradeLevel);
        SchoolYear schoolYear = findSchoolYear(request.getSchoolYearId());
        String code = normalize(request.getCode());
        String name = normalize(request.getName());
        boolean gradeLevelChanged = classroom.getGradeLevel() == null
                || !classroom.getGradeLevel().getId().equals(gradeLevel.getId());

        validateUnique(unit.getId(), gradeLevel.getId(), schoolYear.getId(), code, name, id);

        classroom.setCode(code);
        classroom.setName(name);
        classroom.setUnit(unit);
        classroom.setGradeLevel(gradeLevel);
        classroom.setSchoolYear(schoolYear);
        classroom.setStatus(request.getStatus());
        classroom.setDescription(normalizeNullable(request.getDescription()));
        classroom.setUpdatedBy(SecurityUtils.getCurrentUsername());
        Classroom savedClassroom = classroomRepository.save(classroom);
        if (gradeLevelChanged) {
            classroomSubjectService.syncFromGradeLevel(savedClassroom);
        }
        return toDetailDto(savedClassroom);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Classroom classroom = findClassroom(id);
        validateClassroomScope(ActionType.DELETE, classroom);

        classroomSubjectService.clearByClassroomId(id);
        classroom.setDeletedFlag(1);
        classroom.setDeletedAt(LocalDateTime.now());
        classroom.setDeletedBy(SecurityUtils.getCurrentUsername());
        classroomRepository.save(classroom);
    }

    private Classroom findClassroom(Long id) {
        return classroomRepository.findById(id)
                .orElseThrow(() -> new UserMessageException(CommonErrorCode.CLASS_NOT_FOUND));
    }

    private void validateClassroomScope(ActionType action, Classroom classroom) {
        List<ResolvedScope> resolvedScopes = dataScopeFilterService.getResolvedScopes(FEATURE, action);
        if (!hasClassroomAccess(resolvedScopes, classroom)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "User khong co quyen " + action + " tren classroom trong scope hien tai");
        }
    }

    private void validateClassroomTargetScope(ActionType action, Unit unit, GradeLevel gradeLevel) {
        List<ResolvedScope> resolvedScopes = dataScopeFilterService.getResolvedScopes(FEATURE, action);
        if (!hasClassroomTargetAccess(resolvedScopes, unit, gradeLevel)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "User khong co quyen " + action + " tren classroom target trong scope hien tai");
        }
    }

    private boolean hasClassroomAccess(List<ResolvedScope> resolvedScopes, Classroom classroom) {
        if (resolvedScopes == null || resolvedScopes.isEmpty()) {
            return false;
        }
        return resolvedScopes.stream().anyMatch(scope -> {
            if (scope == null) {
                return false;
            }
            if (scope.isUnrestricted() || scope.getScopeType() == ScopeType.ALL) {
                return true;
            }
            if (scope.getScopeIds() == null || scope.getScopeIds().isEmpty()) {
                return false;
            }
            return switch (scope.getScopeType()) {
                case UNIT -> classroom.getUnit() != null && scope.getScopeIds().contains(classroom.getUnit().getId());
                case CLASS -> scope.getScopeIds().contains(classroom.getId());
                case GRADE -> classroom.getGradeLevel() != null
                        && scope.getScopeIds().contains(classroom.getGradeLevel().getId());
                default -> false;
            };
        });
    }

    private boolean hasClassroomTargetAccess(List<ResolvedScope> resolvedScopes, Unit unit, GradeLevel gradeLevel) {
        if (resolvedScopes == null || resolvedScopes.isEmpty()) {
            return false;
        }
        return resolvedScopes.stream().anyMatch(scope -> {
            if (scope == null) {
                return false;
            }
            if (scope.isUnrestricted() || scope.getScopeType() == ScopeType.ALL) {
                return true;
            }
            if (scope.getScopeIds() == null || scope.getScopeIds().isEmpty()) {
                return false;
            }
            return switch (scope.getScopeType()) {
                case UNIT -> unit != null && scope.getScopeIds().contains(unit.getId());
                case GRADE -> gradeLevel != null && scope.getScopeIds().contains(gradeLevel.getId());
                case CLASS -> false;
                default -> false;
            };
        });
    }

    private Unit findUnit(Long id) {
        return unitRepository.findById(id)
                .orElseThrow(() -> new UserMessageException(CommonErrorCode.UNIT_NOT_FOUND));
    }

    private GradeLevel findGradeLevel(Long id) {
        return gradeLevelRepository.findById(id)
                .orElseThrow(() -> new UserMessageException(CommonErrorCode.GRADE_LEVEL_NOT_FOUND));
    }

    private SchoolYear findSchoolYear(Long id) {
        return schoolYearRepository.findById(id)
                .orElseThrow(() -> new UserMessageException(CommonErrorCode.SCHOOL_YEAR_NOT_FOUND));
    }

    private void validateUnique(Long unitId, Long gradeLevelId, Long schoolYearId, String code, String name, Long id) {
        classroomRepository.findByUnitIdAndGradeLevelIdAndSchoolYearIdAndCode(unitId, gradeLevelId, schoolYearId, code)
                .filter(item -> id == null || !item.getId().equals(id))
                .ifPresent(item -> {
                    throw new UserMessageException(CommonErrorCode.CLASS_CODE_ALREADY_EXISTS);
                });
        classroomRepository.findByUnitIdAndGradeLevelIdAndSchoolYearIdAndName(unitId, gradeLevelId, schoolYearId, name)
                .filter(item -> id == null || !item.getId().equals(id))
                .ifPresent(item -> {
                    throw new UserMessageException(CommonErrorCode.CLASS_NAME_ALREADY_EXISTS);
                });
    }

    private ClassroomDetailDto toDetailDto(Classroom classroom) {
        return ClassroomDetailDto.builder()
                .id(classroom.getId())
                .code(classroom.getCode())
                .name(classroom.getName())
                .unitId(classroom.getUnit() == null ? null : classroom.getUnit().getId())
                .gradeLevelId(classroom.getGradeLevel() == null ? null : classroom.getGradeLevel().getId())
                .schoolYearId(classroom.getSchoolYear() == null ? null : classroom.getSchoolYear().getId())
                .status(classroom.getStatus())
                .description(classroom.getDescription())
                .build();
    }

    private ClassroomListItemDto toListItemDto(Classroom classroom) {
        return ClassroomListItemDto.builder()
                .id(classroom.getId())
                .code(classroom.getCode())
                .name(classroom.getName())
                .unitName(classroom.getUnit() == null ? null : classroom.getUnit().getName())
                .gradeLevelName(classroom.getGradeLevel() == null ? null : classroom.getGradeLevel().getName())
                .schoolYearName(classroom.getSchoolYear() == null ? null : classroom.getSchoolYear().getName())
                .status(classroom.getStatus())
                .build();
    }

    private int normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize <= 0) {
            return 20;
        }
        return Math.min(pageSize, 100);
    }

    private int normalizePageNow(Integer pageNow) {
        if (pageNow == null || pageNow < 0) {
            return 0;
        }
        return pageNow;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }

    private String normalizeNullable(String value) {
        return hasText(value) ? value.trim() : null;
    }
}
