package com.gfi.backend.services.implement;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gfi.backend.controllers.exceptions.UserMessageException;
import com.gfi.backend.models.dtos.classroom.ClassroomCreateRequest;
import com.gfi.backend.models.dtos.classroom.ClassroomDetailDto;
import com.gfi.backend.models.dtos.classroom.ClassroomFilterDto;
import com.gfi.backend.models.dtos.classroom.ClassroomListItemDto;
import com.gfi.backend.models.dtos.classroom.ClassroomUpdateRequest;
import com.gfi.backend.models.dtos.common.LookupItemDto;
import com.gfi.backend.models.dtos.common.PageRequestDto;
import com.gfi.backend.models.dtos.common.PageResponseDto;
import com.gfi.backend.models.entities.Classroom;
import com.gfi.backend.models.entities.GradeLevel;
import com.gfi.backend.models.entities.SchoolYear;
import com.gfi.backend.models.entities.Unit;
import com.gfi.backend.models.global.CommonErrorCode;
import com.gfi.backend.repositories.ClassroomRepository;
import com.gfi.backend.repositories.GradeLevelRepository;
import com.gfi.backend.repositories.SchoolYearRepository;
import com.gfi.backend.repositories.UnitRepository;
import com.gfi.backend.repositories.specifications.ClassroomSpecification;
import com.gfi.backend.services.interfaces.ClassroomService;
import com.gfi.backend.services.interfaces.ClassroomSubjectService;
import com.gfi.backend.utils.PageableUtils;
import com.gfi.backend.utils.SecurityUtils;
import com.gfi.backend.utils.ScopeFilterUtils;

import lombok.RequiredArgsConstructor;

/**
 * Service xử lý logic quản lý lớp học.
 * 
 * Trách nhiệm tách biệt:
 * - Logic query: ClassroomSpecification
 * - Validate & load relations: private helpers
 * - Security: SecurityUtils
 */
@Service
@RequiredArgsConstructor
public class ClassroomServiceImpl implements ClassroomService {

    private final ClassroomRepository classroomRepository;
    private final UnitRepository unitRepository;
    private final GradeLevelRepository gradeLevelRepository;
    private final SchoolYearRepository schoolYearRepository;
    private final ClassroomSubjectService classroomSubjectService;
    private final ClassroomSpecification classroomSpecification;

    // Tìm kiếm và phân trang lớp học với filter
    @Override
    @Transactional(readOnly = true)
    public PageResponseDto<ClassroomListItemDto, ClassroomFilterDto> search(
            PageRequestDto<ClassroomFilterDto> request) {
        ClassroomFilterDto filter = request.getFilter() == null ? new ClassroomFilterDto() : request.getFilter();
        int pageSize = normalizePageSize(request.getPageSize());
        int pageNow = normalizePageNow(request.getPageNow());
        Pageable pageable = PageableUtils.newestFirst(pageNow, pageSize);

        // Lấy danh sách unitId được phép truy cập từ scope để áp dụng filter
        List<Long> allowedUnitIds = ScopeFilterUtils.getScopesForQuery("CLASS_MANAGEMENT");

        Page<ClassroomListItemDto> page;
        if (ScopeFilterUtils.isScopeUnrestricted(allowedUnitIds)) {
            // Unrestricted (ALL scope): use specification as is
            Page<Classroom> classrooms = classroomRepository.findAll(classroomSpecification.buildSpecification(filter),
                    pageable);
            page = classrooms.map(this::toListItemDto);
        } else {
            // Restricted: filter by allowed units directly
            Page<Classroom> classrooms = classroomRepository.findByUnitIdIn(allowedUnitIds, pageable);
            page = classrooms.map(this::toListItemDto);
        }

        return PageResponseDto.<ClassroomListItemDto, ClassroomFilterDto>builder()
                .pageSize(pageSize)
                .pageNow(pageNow)
                .filter(filter)
                .pageTotal(page.getTotalPages())
                .recordTotal(page.getTotalElements())
                .items(page.getContent())
                .build();
    }

    // Lấy danh sách lớp học theo các bộ lọc để dropdown
    @Override
    @Transactional(readOnly = true)
    public List<LookupItemDto> getOptions(Long unitId, Long gradeLevelId, Long schoolYearId) {
        return classroomRepository.findByUnitIdAndGradeLevelIdAndSchoolYearId(unitId, gradeLevelId, schoolYearId)
                .stream()
                .map(item -> LookupItemDto.builder().id(item.getId()).name(item.getName()).build())
                .toList();
    }

    // Chi tiết lớp học theo ID
    @Override
    @Transactional(readOnly = true)
    public ClassroomDetailDto getById(Long id) {
        Classroom classroom = findClassroom(id);

        // Enforce scope: validate classroom's unit is within allowed scopes
        ScopeFilterUtils.validateAccess("CLASS_MANAGEMENT", classroom.getUnit().getId());

        return toDetailDto(classroom);
    }

    // Thêm mới lớp học
    @Override
    @Transactional
    public ClassroomDetailDto create(ClassroomCreateRequest request) {
        Unit unit = findUnit(request.getUnitId());
        GradeLevel gradeLevel = findGradeLevel(request.getGradeLevelId());
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

    // Cập nhật lớp học
    @Override
    @Transactional
    public ClassroomDetailDto update(Long id, ClassroomUpdateRequest request) {
        Classroom classroom = findClassroom(id);

        // Enforce scope: validate classroom's unit is within allowed scopes before
        // allowing update
        ScopeFilterUtils.validateAccess("CLASS_MANAGEMENT", classroom.getUnit().getId());

        Unit unit = findUnit(request.getUnitId());
        GradeLevel gradeLevel = findGradeLevel(request.getGradeLevelId());
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

    // Xóa lớp học (soft delete). Kiểm tra không được xóa nếu còn học sinh hoặc cấu
    // hình môn học.
    @Override
    @Transactional
    public void delete(Long id) {
        Classroom classroom = findClassroom(id);

        // Enforce scope: validate classroom's unit is within allowed scopes before
        // allowing delete
        ScopeFilterUtils.validateAccess("CLASS_MANAGEMENT", classroom.getUnit().getId());

        classroomSubjectService.clearByClassroomId(id);

        // Xóa mềm: đánh dấu xóa thay vì hard delete
        classroom.setDeletedFlag(1);
        classroom.setDeletedAt(LocalDateTime.now());
        classroom.setDeletedBy(SecurityUtils.getCurrentUsername());
        classroomRepository.save(classroom);
    }

    private Classroom findClassroom(Long id) {
        return classroomRepository.findById(id)
                .orElseThrow(() -> new UserMessageException(CommonErrorCode.CLASS_NOT_FOUND));
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

    // Kiểm tra mã lớp học phải duy nhất
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

    /**
     * Chuẩn hóa kích thước trang phân trang.
     */
    private int normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize <= 0) {
            return 20; // Default page size
        }
        return Math.min(pageSize, 100); // Max 100 items per page
    }

    /**
     * Chuẩn hóa số trang hiện tại.
     */
    private int normalizePageNow(Integer pageNow) {
        if (pageNow == null || pageNow < 0) {
            return 0; // Default first page
        }
        return pageNow;
    }

    /**
     * Kiểm tra string có nội dung hay không.
     */
    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    /**
     * Chuẩn hóa string: trim.
     */
    private String normalize(String value) {
        return value == null ? null : value.trim();
    }

    /**
     * Chuẩn hóa string nullable: return null nếu rỗng hoặc whitespace.
     */
    private String normalizeNullable(String value) {
        return hasText(value) ? value.trim() : null;
    }
}
