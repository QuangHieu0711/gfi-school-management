package com.gfi.backend.services.implement;

import com.gfi.backend.models.dtos.staff.TeacherAssignmentItemDto;
import com.gfi.backend.models.dtos.staff.TeacherAssignmentFilterDto;
import com.gfi.backend.models.dtos.staff.TeacherAssignmentCreateRequest;
import com.gfi.backend.models.dtos.staff.TeacherAssignmentDetailRequest;
import com.gfi.backend.models.dtos.staff.TeacherAssignmentDetailResponse;
import com.gfi.backend.models.dtos.common.PageRequestDto;
import com.gfi.backend.models.dtos.common.PageResponseDto;
import com.gfi.backend.models.entities.*;
import com.gfi.backend.models.global.CommonErrorCode;
import com.gfi.backend.controllers.exceptions.UserMessageException;
import com.gfi.backend.repositories.*;
import com.gfi.backend.services.interfaces.TeacherAssignmentService;
import com.gfi.backend.utils.PageableUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TeacherAssignmentServiceImpl implements TeacherAssignmentService {

    private final TeacherAssignmentRepository teacherAssignmentRepository;
    private final StaffRepository staffRepository;
    private final SchoolYearRepository schoolYearRepository;
    private final ClassroomRepository classroomRepository;
    private final SubjectRepository subjectRepository;
    private final ClassroomSubjectRepository classroomSubjectRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResponseDto<TeacherAssignmentItemDto, TeacherAssignmentFilterDto> search(PageRequestDto<TeacherAssignmentFilterDto> request) {
        TeacherAssignmentFilterDto filter = request.getFilter() == null ? new TeacherAssignmentFilterDto() : request.getFilter();
        int pageSize = normalizePageSize(request.getPageSize());
        int pageNow = normalizePageNow(request.getPageNow());
        Pageable pageable = PageableUtils.newestFirst(pageNow, pageSize);

        Page<TeacherAssignment> page = teacherAssignmentRepository.findAll(buildSpecification(filter), pageable);
        List<TeacherAssignmentItemDto> items = page.getContent().stream()
                .map(this::toItemDto)
                .toList();

        return PageResponseDto.<TeacherAssignmentItemDto, TeacherAssignmentFilterDto>builder()
                .pageSize(pageSize)
                .pageNow(pageNow)
                .filter(filter)
                .pageTotal(page.getTotalPages())
                .recordTotal(page.getTotalElements())
                .items(items)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public TeacherAssignmentDetailResponse getDetail(TeacherAssignmentDetailRequest request) {
        Staff staff = staffRepository.findById(request.getStaffId())
                .orElseThrow(() -> new UserMessageException(CommonErrorCode.STAFF_NOT_FOUND));
        schoolYearRepository.findById(request.getSchoolYearId())
                .orElseThrow(() -> new UserMessageException(CommonErrorCode.SCHOOL_YEAR_NOT_FOUND));
        subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new UserMessageException(CommonErrorCode.SUBJECT_NOT_FOUND));

        if (!staff.getUnit().getId().equals(request.getUnitId())) {
            throw new UserMessageException("Cán bộ không thuộc đơn vị được chọn");
        }

        List<TeacherAssignment> assignments = teacherAssignmentRepository.findAll((root, query, cb) -> cb.and(
                cb.equal(root.get("staff").get("id"), request.getStaffId()),
                cb.equal(root.get("schoolYear").get("id"), request.getSchoolYearId()),
                cb.equal(root.get("subject").get("id"), request.getSubjectId()),
                cb.equal(root.get("classroom").get("unit").get("id"), request.getUnitId())),
                Sort.by(Sort.Direction.ASC, "classroom.id"));

        if (assignments.isEmpty()) {
            throw new UserMessageException(CommonErrorCode.RECORD_NOT_FOUND);
        }

        List<Long> classIds = assignments.stream()
                .map(a -> a.getClassroom() != null ? a.getClassroom().getId() : null)
                .filter(Objects::nonNull)
                .toList();

        return TeacherAssignmentDetailResponse.builder()
                .unitId(request.getUnitId())
                .staffId(request.getStaffId())
                .schoolYearId(request.getSchoolYearId())
                .subjectId(request.getSubjectId())
                .classIds(classIds)
                .build();
    }

    @Override
    @Transactional
    public List<TeacherAssignmentItemDto> create(TeacherAssignmentCreateRequest request) {
        AssignmentBuildContext context = validateAndBuildContext(request);

        List<TeacherAssignment> existingAssignments = teacherAssignmentRepository
                .findByStaffIdAndSchoolYearIdAndClassroomIdIn(
                        context.staff().getId(),
                        context.schoolYear().getId(),
                        context.classroomIds());

        if (!existingAssignments.isEmpty()) {
            Set<Long> existingClassIds = existingAssignments.stream()
                    .map(item -> item.getClassroom() != null ? item.getClassroom().getId() : null)
                    .filter(Objects::nonNull)
                    .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
            throw new UserMessageException("Đã tồn tại phân công cho các lớp: " + existingClassIds);
        }

        return saveAssignments(context);
    }

    @Override
    @Transactional
    public List<TeacherAssignmentItemDto> update(TeacherAssignmentCreateRequest request) {
        AssignmentBuildContext context = validateAndBuildContext(request);

        List<TeacherAssignment> existingAssignments = teacherAssignmentRepository
                .findByStaffIdAndSchoolYearId(context.staff().getId(), context.schoolYear().getId());
        if (!existingAssignments.isEmpty()) {
            teacherAssignmentRepository.deleteAll(existingAssignments);
        }

        return saveAssignments(context);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        TeacherAssignment assignment = findAssignment(id);
        teacherAssignmentRepository.delete(assignment);
    }

    private Specification<TeacherAssignment> buildSpecification(TeacherAssignmentFilterDto filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new java.util.ArrayList<>();

            if (filter.getStaffId() != null) {
                predicates.add(cb.equal(root.get("staff").get("id"), filter.getStaffId()));
            }
            if (filter.getSchoolYearId() != null) {
                predicates.add(cb.equal(root.get("schoolYear").get("id"), filter.getSchoolYearId()));
            }
            if (filter.getClassId() != null) {
                predicates.add(cb.equal(root.get("classroom").get("id"), filter.getClassId()));
            }
            if (filter.getSubjectId() != null) {
                predicates.add(cb.equal(root.get("subject").get("id"), filter.getSubjectId()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private TeacherAssignmentItemDto toItemDto(TeacherAssignment assignment) {
        return TeacherAssignmentItemDto.builder()
                .id(assignment.getId())
                .staffId(assignment.getStaff().getId())
                .schoolYearId(assignment.getSchoolYear().getId())
                .classId(assignment.getClassroom() != null ? assignment.getClassroom().getId() : null)
                .subjectId(assignment.getSubject() != null ? assignment.getSubject().getId() : null)
                .build();
    }

    private TeacherAssignment findAssignment(Long id) {
        return teacherAssignmentRepository.findById(id)
                .orElseThrow(() -> new UserMessageException(CommonErrorCode.TEACHER_NOT_FOUND));
    }

    private List<TeacherAssignmentItemDto> saveAssignments(AssignmentBuildContext context) {
        List<TeacherAssignment> assignments = new ArrayList<>();

        for (AssignmentSeed seed : context.seeds()) {
            TeacherAssignment assignment = new TeacherAssignment();
            assignment.setStaff(context.staff());
            assignment.setSchoolYear(context.schoolYear());
            assignment.setClassroom(seed.classroom());
            assignment.setSubject(seed.subject());
            assignments.add(assignment);
        }

        return teacherAssignmentRepository.saveAll(assignments).stream()
                .map(this::toItemDto)
                .toList();
    }

    private AssignmentBuildContext validateAndBuildContext(TeacherAssignmentCreateRequest request) {
        Staff staff = staffRepository.findById(request.getStaffId())
                .orElseThrow(() -> new UserMessageException(CommonErrorCode.STAFF_NOT_FOUND));
        SchoolYear schoolYear = schoolYearRepository.findById(request.getSchoolYearId())
                .orElseThrow(() -> new UserMessageException(CommonErrorCode.SCHOOL_YEAR_NOT_FOUND));

        if (!staff.getUnit().getId().equals(request.getUnitId())) {
            throw new UserMessageException("Cán bộ không thuộc đơn vị được chọn");
        }

        List<AssignmentSeed> seeds = new ArrayList<>();
        Set<Long> classroomIds = new LinkedHashSet<>();
        Set<Long> duplicateClassIds = new LinkedHashSet<>();
        Map<Long, Subject> subjectMap = new HashMap<>();
        Map<Long, Classroom> classroomMap = new HashMap<>();

        for (TeacherAssignmentCreateRequest.SubjectAssignmentRequest subjectAssignment : request.getAssignments()) {
            Subject subject = subjectMap.computeIfAbsent(
                    subjectAssignment.getSubjectId(),
                    this::findSubjectOrThrow);

            for (Long classId : subjectAssignment.getClassIds()) {
                Classroom classroom = classroomMap.computeIfAbsent(classId, this::findClassroomOrThrow);
                validateClassroomContext(classroom, request.getUnitId(), request.getSchoolYearId());

                if (!classroomSubjectRepository.existsByClassroomIdAndSubjectId(classroom.getId(), subject.getId())) {
                    throw new UserMessageException(
                            "Môn " + subject.getId() + " chưa được cấu hình cho lớp " + classroom.getId());
                }

                if (!classroomIds.add(classroom.getId())) {
                    duplicateClassIds.add(classroom.getId());
                }

                seeds.add(new AssignmentSeed(classroom, subject));
            }
        }

        if (!duplicateClassIds.isEmpty()) {
            throw new UserMessageException(
                    "Một lớp chỉ được phân công một môn cho cùng giáo viên trong cùng năm học. Lớp bị trùng: "
                            + duplicateClassIds);
        }

        return new AssignmentBuildContext(staff, schoolYear, seeds, new ArrayList<>(classroomIds));
    }

    private Subject findSubjectOrThrow(Long subjectId) {
        return subjectRepository.findById(subjectId)
                .orElseThrow(() -> new UserMessageException(CommonErrorCode.SUBJECT_NOT_FOUND));
    }

    private Classroom findClassroomOrThrow(Long classroomId) {
        return classroomRepository.findById(classroomId)
                .orElseThrow(() -> new UserMessageException(CommonErrorCode.CLASS_NOT_FOUND));
    }

    private void validateClassroomContext(Classroom classroom, Long unitId, Long schoolYearId) {
        if (!classroom.getUnit().getId().equals(unitId)) {
            throw new UserMessageException("Lớp " + classroom.getId() + " không thuộc đơn vị đã chọn");
        }
        if (!classroom.getSchoolYear().getId().equals(schoolYearId)) {
            throw new UserMessageException("Lớp " + classroom.getId() + " không thuộc năm học đã chọn");
        }
    }

    private record AssignmentSeed(Classroom classroom, Subject subject) {
    }

    private record AssignmentBuildContext(Staff staff, SchoolYear schoolYear, List<AssignmentSeed> seeds,
            List<Long> classroomIds) {
    }

    private int normalizePageSize(Integer pageSize) {
        return pageSize == null || pageSize <= 0 ? 20 : pageSize;
    }

    private int normalizePageNow(Integer pageNow) {
        return pageNow == null || pageNow < 0 ? 0 : pageNow;
    }
}
