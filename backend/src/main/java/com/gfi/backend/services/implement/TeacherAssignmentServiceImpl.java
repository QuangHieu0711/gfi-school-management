package com.gfi.backend.services.implement;

import com.gfi.backend.models.dtos.staff.TeacherAssignmentItemDto;
import com.gfi.backend.models.dtos.staff.TeacherAssignmentFilterDto;
import com.gfi.backend.models.dtos.staff.TeacherAssignmentCreateRequest;
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
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TeacherAssignmentServiceImpl implements TeacherAssignmentService {

    private final TeacherAssignmentRepository teacherAssignmentRepository;
    private final StaffRepository staffRepository;
    private final SchoolYearRepository schoolYearRepository;
    private final ClassroomRepository classroomRepository;
    private final SubjectRepository subjectRepository;

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
    public TeacherAssignmentItemDto getById(Long id) {
        TeacherAssignment assignment = findAssignment(id);
        return toItemDto(assignment);
    }

    @Override
    @Transactional
    public TeacherAssignmentItemDto create(TeacherAssignmentCreateRequest request) {
        Staff staff = staffRepository.findById(request.getStaffId())
                .orElseThrow(() -> new UserMessageException(CommonErrorCode.STAFF_NOT_FOUND));
        SchoolYear schoolYear = schoolYearRepository.findById(request.getSchoolYearId())
                .orElseThrow(() -> new UserMessageException(CommonErrorCode.SCHOOL_YEAR_NOT_FOUND));

        TeacherAssignment assignment = new TeacherAssignment();
        assignment.setStaff(staff);
        assignment.setSchoolYear(schoolYear);

        if (request.getClassId() != null) {
            Classroom classroom = classroomRepository.findById(request.getClassId())
                    .orElseThrow(() -> new UserMessageException(CommonErrorCode.CLASS_NOT_FOUND));
            assignment.setClassroom(classroom);
        }

        if (request.getSubjectId() != null) {
            Subject subject = subjectRepository.findById(request.getSubjectId())
                    .orElseThrow(() -> new UserMessageException(CommonErrorCode.SUBJECT_NOT_FOUND));
            assignment.setSubject(subject);
        }

        assignment.setIsHomeroom(request.getIsHomeroom() != null ? request.getIsHomeroom() : false);
        assignment.setDepartmentId(request.getDepartmentId());
        assignment.setTeachingLoad(request.getTeachingLoad());
        assignment.setNote(request.getNote());

        TeacherAssignment saved = teacherAssignmentRepository.save(assignment);
        return toItemDto(saved);
    }

    @Override
    @Transactional
    public TeacherAssignmentItemDto update(Long id, TeacherAssignmentCreateRequest request) {
        TeacherAssignment assignment = findAssignment(id);

        if (request.getClassId() != null) {
            Classroom classroom = classroomRepository.findById(request.getClassId())
                    .orElseThrow(() -> new UserMessageException(CommonErrorCode.CLASS_NOT_FOUND));
            assignment.setClassroom(classroom);
        } else {
            assignment.setClassroom(null);
        }

        if (request.getSubjectId() != null) {
            Subject subject = subjectRepository.findById(request.getSubjectId())
                    .orElseThrow(() -> new UserMessageException(CommonErrorCode.SUBJECT_NOT_FOUND));
            assignment.setSubject(subject);
        } else {
            assignment.setSubject(null);
        }

        assignment.setIsHomeroom(request.getIsHomeroom() != null ? request.getIsHomeroom() : false);
        assignment.setDepartmentId(request.getDepartmentId());
        assignment.setTeachingLoad(request.getTeachingLoad());
        assignment.setNote(request.getNote());

        TeacherAssignment saved = teacherAssignmentRepository.save(assignment);
        return toItemDto(saved);
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
            if (filter.getIsHomeroom() != null) {
                predicates.add(cb.equal(root.get("isHomeroom"), filter.getIsHomeroom()));
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
                .isHomeroom(assignment.getIsHomeroom())
                .departmentId(assignment.getDepartmentId())
                .teachingLoad(assignment.getTeachingLoad())
                .build();
    }

    private TeacherAssignment findAssignment(Long id) {
        return teacherAssignmentRepository.findById(id)
                .orElseThrow(() -> new UserMessageException(CommonErrorCode.TEACHER_NOT_FOUND));
    }

    private int normalizePageSize(Integer pageSize) {
        return pageSize == null || pageSize <= 0 ? 20 : pageSize;
    }

    private int normalizePageNow(Integer pageNow) {
        return pageNow == null || pageNow < 0 ? 0 : pageNow;
    }
}
