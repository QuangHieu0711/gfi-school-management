package com.gfi.backend.services.implement;

import com.gfi.backend.controllers.exceptions.UserMessageException;
import com.gfi.backend.models.dtos.staff.TeacherAssignmentAssignmentDto;
import com.gfi.backend.models.dtos.staff.TeacherAssignmentCreateRequest;
import com.gfi.backend.models.dtos.staff.TeacherAssignmentDetailRequest;
import com.gfi.backend.models.dtos.staff.TeacherAssignmentDetailResponse;
import com.gfi.backend.models.dtos.staff.TeacherAssignmentFilterDto;
import com.gfi.backend.models.dtos.staff.TeacherAssignmentImportResultDto;
import com.gfi.backend.models.dtos.staff.TeacherAssignmentItemDto;
import com.gfi.backend.models.dtos.staff.TeacherAssignmentSearchRequest;
import com.gfi.backend.models.dtos.staff.TeacherAssignmentSearchResponse;
import com.gfi.backend.models.dtos.staff.TeacherAssignmentSearchStaffItemDto;
import com.gfi.backend.models.entities.Classroom;
import com.gfi.backend.models.entities.ClassroomSubject;
import com.gfi.backend.models.entities.SchoolYear;
import com.gfi.backend.models.entities.Semester;
import com.gfi.backend.models.entities.Staff;
import com.gfi.backend.models.entities.Subject;
import com.gfi.backend.models.entities.TeacherAssignment;
import com.gfi.backend.models.entities.Unit;
import com.gfi.backend.models.global.CommonErrorCode;
import com.gfi.backend.repositories.ClassroomRepository;
import com.gfi.backend.repositories.ClassroomSubjectRepository;
import com.gfi.backend.repositories.SchoolYearRepository;
import com.gfi.backend.repositories.SemesterRepository;
import com.gfi.backend.repositories.StaffRepository;
import com.gfi.backend.repositories.SubjectRepository;
import com.gfi.backend.repositories.TeacherAssignmentRepository;
import com.gfi.backend.repositories.UnitRepository;
import com.gfi.backend.services.interfaces.TeacherAssignmentService;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import jakarta.persistence.criteria.Predicate;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TeacherAssignmentServiceImpl implements TeacherAssignmentService {

    private static final int HEADER_ROW_INDEX = 5;
    private static final int DATA_START_ROW_INDEX = 6;
    private static final Pattern ASSIGNMENT_PATTERN = Pattern.compile("^([^()]+)\\(([^()]*)\\)$");

    private final TeacherAssignmentRepository teacherAssignmentRepository;
    private final StaffRepository staffRepository;
    private final SchoolYearRepository schoolYearRepository;
    private final SemesterRepository semesterRepository;
    private final ClassroomRepository classroomRepository;
    private final SubjectRepository subjectRepository;
    private final UnitRepository unitRepository;
    private final ClassroomSubjectRepository classroomSubjectRepository;

    @Override
    @Transactional(readOnly = true)
    public TeacherAssignmentSearchResponse search(TeacherAssignmentSearchRequest request) {
        TeacherAssignmentFilterDto filter = request.getFilter() == null ? new TeacherAssignmentFilterDto() : request.getFilter();
        int pageSize = normalizePageSize(request.getPageSize());
        int pageNow = normalizePageNow(request.getPageNow());
        List<TeacherAssignmentSearchStaffItemDto> distinctItems = teacherAssignmentRepository
                .findAll(buildSpecification(filter), Sort.by(Sort.Direction.DESC, "id"))
                .stream()
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(
                        this::buildStaffSearchKey,
                        LinkedHashMap::new,
                        Collectors.toList()))
                .values()
                .stream()
                .map(this::toSearchStaffItemDto)
                .toList();

        long totalItems = distinctItems.size();
        int totalPages = totalItems == 0 ? 0 : (int) Math.ceil((double) totalItems / pageSize);
        int fromIndex = Math.min((pageNow - 1) * pageSize, distinctItems.size());
        int toIndex = Math.min(fromIndex + pageSize, distinctItems.size());

        return TeacherAssignmentSearchResponse.builder()
                .pageNow(pageNow)
                .pageSize(pageSize)
                .totalItems(totalItems)
                .totalPages(totalPages)
                .items(distinctItems.subList(fromIndex, toIndex))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public TeacherAssignmentDetailResponse getDetail(TeacherAssignmentDetailRequest request) {
        Staff staff = staffRepository.findById(request.getStaffId())
                .orElseThrow(() -> new UserMessageException(CommonErrorCode.STAFF_NOT_FOUND));
        schoolYearRepository.findById(request.getSchoolYearId())
                .orElseThrow(() -> new UserMessageException(CommonErrorCode.SCHOOL_YEAR_NOT_FOUND));
        Semester semester = findSemesterOrThrow(request.getSemesterId());
        subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new UserMessageException(CommonErrorCode.SUBJECT_NOT_FOUND));

        if (!staff.getUnit().getId().equals(request.getUnitId())) {
            throw new UserMessageException("Cán bộ không thuộc đơn vị được chọn");
        }
        validateSemesterBelongsToSchoolYear(semester, request.getSchoolYearId());

        List<TeacherAssignment> assignments = teacherAssignmentRepository.findAll((root, query, cb) -> cb.and(
                cb.equal(root.get("staff").get("id"), request.getStaffId()),
                cb.equal(root.get("schoolYear").get("id"), request.getSchoolYearId()),
                cb.equal(root.get("semester").get("id"), request.getSemesterId()),
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
                .semesterId(request.getSemesterId())
                .subjectId(request.getSubjectId())
                .classIds(classIds)
                .build();
    }

    @Override
    @Transactional
    public List<TeacherAssignmentItemDto> create(TeacherAssignmentCreateRequest request) {
        AssignmentBuildContext context = validateAndBuildContext(request);

        List<TeacherAssignment> existingAssignments = teacherAssignmentRepository
                .findByStaffIdAndSchoolYearIdAndSemesterIdAndClassroomIdIn(
                        context.staff().getId(),
                        context.schoolYear().getId(),
                        context.semester().getId(),
                        context.classroomIds());

        if (!existingAssignments.isEmpty()) {
            Set<Long> existingClassIds = existingAssignments.stream()
                    .map(item -> item.getClassroom() != null ? item.getClassroom().getId() : null)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            throw new UserMessageException("Đã tồn tại phân công cho các lớp: " + existingClassIds);
        }

        return saveAssignments(context);
    }

    @Override
    @Transactional
    public List<TeacherAssignmentItemDto> update(TeacherAssignmentCreateRequest request) {
        AssignmentBuildContext context = validateAndBuildContext(request);

        List<TeacherAssignment> existingAssignments = teacherAssignmentRepository
                .findByStaffIdAndSchoolYearIdAndSemesterId(context.staff().getId(), context.schoolYear().getId(),
                        context.semester().getId());
        if (!existingAssignments.isEmpty()) {
            teacherAssignmentRepository.deleteAll(existingAssignments);
        }

        return saveAssignments(context);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportExcelTemplate(Long schoolYearId, Long unitId) {
        ExportContext context = buildExportContext(schoolYearId, unitId);

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            buildAssignmentSheet(workbook.createSheet("PCGD"), workbook, context);
            buildSubjectSheet(workbook.createSheet("MonHoc"), workbook, context.subjects());
            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException ex) {
            throw new UserMessageException("Không thể tạo file Excel phân công giảng dạy");
        }
    }

    @Override
    @Transactional
    public TeacherAssignmentImportResultDto importExcel(Long schoolYearId, Long unitId, MultipartFile file) {
        validateExcelFile(file);
        ExportContext context = buildExportContext(schoolYearId, unitId);
        List<TeacherAssignment> assignments = readAssignmentsFromExcel(file, context);

        if (context.semesterOne() != null) {
            teacherAssignmentRepository.deleteBySchoolYearIdAndSemesterIdAndClassroom_Unit_Id(
                    schoolYearId, context.semesterOne().getId(), unitId);
        }
        if (context.semesterTwo() != null) {
            teacherAssignmentRepository.deleteBySchoolYearIdAndSemesterIdAndClassroom_Unit_Id(
                    schoolYearId, context.semesterTwo().getId(), unitId);
        }

        List<TeacherAssignment> saved = teacherAssignmentRepository.saveAll(assignments);
        return TeacherAssignmentImportResultDto.builder()
                .successCount(saved.size())
                .failedCount(0)
                .build();
    }

    @Override
    @Transactional
    public void delete(Long id) {
        TeacherAssignment assignment = findAssignment(id);
        teacherAssignmentRepository.delete(assignment);
    }

    private Specification<TeacherAssignment> buildSpecification(TeacherAssignmentFilterDto filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter.getStaffId() != null) {
                predicates.add(cb.equal(root.get("staff").get("id"), filter.getStaffId()));
            }
            if (filter.getUnitId() != null) {
                predicates.add(cb.equal(root.get("classroom").get("unit").get("id"), filter.getUnitId()));
            }
            if (filter.getSchoolYearId() != null) {
                predicates.add(cb.equal(root.get("schoolYear").get("id"), filter.getSchoolYearId()));
            }
            if (filter.getSemesterId() != null) {
                predicates.add(cb.equal(root.get("semester").get("id"), filter.getSemesterId()));
            }
            if (filter.getClassId() != null) {
                predicates.add(cb.equal(root.get("classroom").get("id"), filter.getClassId()));
            }
            if (filter.getSubjectId() != null) {
                predicates.add(cb.equal(root.get("subject").get("id"), filter.getSubjectId()));
            }
            if (StringUtils.hasText(filter.getStaffCode())) {
                predicates.add(cb.equal(cb.lower(root.get("staff").get("staffCode")),
                        filter.getStaffCode().trim().toLowerCase(Locale.ROOT)));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private TeacherAssignmentSearchStaffItemDto toSearchStaffItemDto(List<TeacherAssignment> assignments) {
        TeacherAssignment assignment = assignments.get(0);
        Classroom classroom = assignment.getClassroom();
        Staff staff = assignment.getStaff();
        Semester semester = assignment.getSemester();

        List<TeacherAssignmentAssignmentDto> assignmentItems = assignments.stream()
                .collect(Collectors.groupingBy(
                        item -> item.getSubject() != null ? item.getSubject().getId() : null,
                        LinkedHashMap::new,
                        Collectors.toList()))
                .values()
                .stream()
                .map(this::toAssignmentDto)
                .toList();

        return TeacherAssignmentSearchStaffItemDto.builder()
                .unitId(classroom != null && classroom.getUnit() != null
                        ? classroom.getUnit().getId()
                        : (staff.getUnit() != null ? staff.getUnit().getId() : null))
                .schoolYearId(assignment.getSchoolYear().getId())
                .semesterId(semester != null ? semester.getId() : null)
                .semesterName(semester != null ? semester.getName() : null)
                .staffId(staff.getId())
                .staffCode(staff.getStaffCode())
                .staffName(staff.getFullName())
                .assignments(assignmentItems)
                .build();
    }

    private TeacherAssignmentAssignmentDto toAssignmentDto(List<TeacherAssignment> assignments) {
        TeacherAssignment assignment = assignments.get(0);
        Subject subject = assignment.getSubject();

        List<Long> classIds = assignments.stream()
                .map(TeacherAssignment::getClassroom)
                .filter(Objects::nonNull)
                .map(Classroom::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        List<String> classNames = assignments.stream()
                .map(TeacherAssignment::getClassroom)
                .filter(Objects::nonNull)
                .map(Classroom::getName)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();

        return TeacherAssignmentAssignmentDto.builder()
                .subjectId(subject != null ? subject.getId() : null)
                .subjectName(subject != null ? subject.getName() : null)
                .classIds(classIds)
                .classNames(classNames)
                .build();
    }

    private String buildStaffSearchKey(TeacherAssignment assignment) {
        Staff staff = assignment.getStaff();
        Classroom classroom = assignment.getClassroom();
        return String.join("|",
                String.valueOf(classroom != null && classroom.getUnit() != null
                        ? classroom.getUnit().getId()
                        : (staff != null && staff.getUnit() != null ? staff.getUnit().getId() : null)),
                String.valueOf(staff != null ? staff.getId() : null),
                String.valueOf(assignment.getSchoolYear() != null ? assignment.getSchoolYear().getId() : null),
                String.valueOf(assignment.getSemester() != null ? assignment.getSemester().getId() : null));
    }

    private TeacherAssignmentItemDto toItemDto(TeacherAssignment assignment) {
        return TeacherAssignmentItemDto.builder()
                .id(assignment.getId())
                .staffId(assignment.getStaff().getId())
                .schoolYearId(assignment.getSchoolYear().getId())
                .semesterId(assignment.getSemester().getId())
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
            assignment.setSemester(context.semester());
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
        Semester semester = findSemesterOrThrow(request.getSemesterId());
        validateSemesterBelongsToSchoolYear(semester, schoolYear.getId());

        if (!staff.getUnit().getId().equals(request.getUnitId())) {
            throw new UserMessageException("Cán bộ không thuộc đơn vị được chọn");
        }

        List<AssignmentSeed> seeds = new ArrayList<>();
        Set<Long> classroomIds = new LinkedHashSet<>();
        Set<Long> duplicateClassIds = new LinkedHashSet<>();
        Map<Long, Subject> subjectMap = new HashMap<>();
        Map<Long, Classroom> classroomMap = new HashMap<>();

        for (TeacherAssignmentCreateRequest.SubjectAssignmentRequest subjectAssignment : request.getAssignments()) {
            Subject subject = subjectMap.computeIfAbsent(subjectAssignment.getSubjectId(), this::findSubjectOrThrow);

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
                    "Một lớp chỉ được phân công một môn cho cùng giáo viên trong cùng học kỳ. Lớp bị trùng: "
                            + duplicateClassIds);
        }

        return new AssignmentBuildContext(staff, schoolYear, semester, seeds, new ArrayList<>(classroomIds));
    }

    private ExportContext buildExportContext(Long schoolYearId, Long unitId) {
        SchoolYear schoolYear = schoolYearRepository.findById(schoolYearId)
                .filter(item -> item.getDeletedFlag() == 0)
                .orElseThrow(() -> new UserMessageException(CommonErrorCode.SCHOOL_YEAR_NOT_FOUND));
        Unit unit = unitRepository.findById(unitId)
                .filter(item -> item.getDeletedFlag() == 0)
                .orElseThrow(() -> new UserMessageException(CommonErrorCode.UNIT_NOT_FOUND));

        List<Semester> semesters = semesterRepository.findBySchoolYearId(schoolYearId).stream()
                .filter(item -> item.getDeletedFlag() == 0)
                .sorted(Comparator.comparing(Semester::getSemesterOrder).thenComparing(Semester::getId))
                .toList();
        if (semesters.isEmpty()) {
            throw new UserMessageException("Năm học chưa có học kỳ để import phân công giảng dạy");
        }

        Semester semesterOne = semesters.size() > 0 ? semesters.get(0) : null;
        Semester semesterTwo = semesters.size() > 1 ? semesters.get(1) : null;

        List<Staff> staffs = staffRepository.findAll((root, query, cb) -> cb.and(
                        cb.equal(root.get("unit").get("id"), unitId),
                        cb.equal(root.get("deletedFlag"), 0)),
                Sort.by(Sort.Direction.ASC, "fullName").and(Sort.by(Sort.Direction.ASC, "id")));

        List<Classroom> classrooms = classroomRepository.findAll((root, query, cb) -> cb.and(
                        cb.equal(root.get("unit").get("id"), unitId),
                        cb.equal(root.get("schoolYear").get("id"), schoolYearId),
                        cb.equal(root.get("deletedFlag"), 0)),
                Sort.by(Sort.Direction.ASC, "name").and(Sort.by(Sort.Direction.ASC, "id")));
        Map<String, Classroom> classroomByName = classrooms.stream()
                .collect(Collectors.toMap(item -> normalize(item.getName()), item -> item, (left, right) -> left,
                        LinkedHashMap::new));

        List<Subject> subjects = subjectRepository.findAll(Sort.by(Sort.Direction.ASC, "code").and(Sort.by("id"))).stream()
                .filter(item -> item.getDeletedFlag() == 0)
                .toList();
        Map<String, Subject> subjectByCodeOrName = new LinkedHashMap<>();
        for (Subject subject : subjects) {
            subjectByCodeOrName.put(normalize(subject.getCode()), subject);
            subjectByCodeOrName.putIfAbsent(normalize(subject.getName()), subject);
        }

        Set<Long> validPairs = classroomSubjectRepository.findAll().stream()
                .filter(item -> item.getStatus() != null && item.getStatus() == 1)
                .filter(item -> item.getClassroom() != null && item.getSubject() != null)
                .filter(item -> item.getClassroom().getDeletedFlag() == 0 && item.getSubject().getDeletedFlag() == 0)
                .filter(item -> item.getClassroom().getUnit().getId().equals(unitId))
                .filter(item -> item.getClassroom().getSchoolYear().getId().equals(schoolYearId))
                .map(item -> buildPairKey(item.getClassroom().getId(), item.getSubject().getId()))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<TeacherAssignment> existingAssignments = teacherAssignmentRepository.findAll((root, query, cb) -> cb.and(
                        cb.equal(root.get("schoolYear").get("id"), schoolYearId),
                        cb.equal(root.get("classroom").get("unit").get("id"), unitId)),
                Sort.by(Sort.Direction.ASC, "staff.fullName").and(Sort.by("id")));

        return new ExportContext(schoolYear, unit, semesterOne, semesterTwo, staffs, classrooms, subjects,
                classroomByName, subjectByCodeOrName, validPairs, existingAssignments);
    }

    private void buildAssignmentSheet(Sheet sheet, Workbook workbook, ExportContext context) {
        CellStyle titleStyle = createTitleStyle(workbook);
        CellStyle guideLabelStyle = createGuideLabelStyle(workbook);
        CellStyle guideContentStyle = createGuideContentStyle(workbook);
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle bodyStyle = createBodyStyle(workbook);

        Row row0 = sheet.createRow(0);
        createCell(row0, 7, "SchoolYearId", bodyStyle);
        createCell(row0, 8, context.schoolYear().getId(), bodyStyle);
        Row row1 = sheet.createRow(1);
        createCell(row1, 7, "UnitId", bodyStyle);
        createCell(row1, 8, context.unit().getId(), bodyStyle);
        Row row2 = sheet.createRow(2);
        createCell(row2, 7, "SemesterOneId", bodyStyle);
        createCell(row2, 8, context.semesterOne() != null ? context.semesterOne().getId() : null, bodyStyle);
        Row row3 = sheet.createRow(3);
        createCell(row3, 7, "SemesterTwoId", bodyStyle);
        createCell(row3, 8, context.semesterTwo() != null ? context.semesterTwo().getId() : null, bodyStyle);
        sheet.setColumnHidden(7, true);
        sheet.setColumnHidden(8, true);

        sheet.addMergedRegion(new CellRangeAddress(0, 0, 2, 5));
        createCell(row0, 2, "BẢNG PHÂN CÔNG GIẢNG DẠY", titleStyle);

        for (int rowIndex = 1; rowIndex <= 4; rowIndex++) {
            sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 2, 5));
        }
        createCell(row1, 1, "Hướng dẫn:", guideLabelStyle);
        createCell(row1, 2, "Môn học nhập theo ký hiệu môn học trong sheet {MonHoc}", guideContentStyle);
        createCell(row2, 2,
                "Phân công giảng dạy nhập theo quy tắc: KyHieuMonHoc(ten lop 1, ten lop 2, ...)",
                guideContentStyle);
        createCell(row3, 2,
                "Nhiều môn phân biệt bởi dấu '+'. Ví dụ: TOAN(1A1, 1A2)+AN(1A3)",
                guideContentStyle);
        createCell(row4, 2,
                "Giáo viên không có phân công trong học kỳ thì để trống ô tương ứng",
                guideContentStyle);

        Row headerRow = sheet.createRow(HEADER_ROW_INDEX);
        createCell(headerRow, 0, "STT", headerStyle);
        createCell(headerRow, 1, "Mã giáo viên", headerStyle);
        createCell(headerRow, 2, "Họ và tên giáo viên", headerStyle);
        createCell(headerRow, 3, "Tổ bộ môn", headerStyle);
        createCell(headerRow, 4, buildSemesterHeader(context.semesterOne(), "HK I"), headerStyle);
        createCell(headerRow, 5, buildSemesterHeader(context.semesterTwo(), "HK II"), headerStyle);

        Map<Long, Map<Long, Map<Long, List<String>>>> grouped = new LinkedHashMap<>();
        for (TeacherAssignment item : context.existingAssignments()) {
            if (item.getStaff() == null || item.getSemester() == null || item.getSubject() == null || item.getClassroom() == null) {
                continue;
            }
            grouped.computeIfAbsent(item.getStaff().getId(), key -> new LinkedHashMap<>())
                    .computeIfAbsent(item.getSemester().getId(), key -> new LinkedHashMap<>())
                    .computeIfAbsent(item.getSubject().getId(), key -> new ArrayList<>())
                    .add(item.getClassroom().getName());
        }

        int rowIndex = DATA_START_ROW_INDEX;
        int stt = 1;
        for (Staff staff : context.staffs()) {
            Row row = sheet.createRow(rowIndex++);
            createCell(row, 0, stt++, bodyStyle);
            createCell(row, 1, staff.getStaffCode(), bodyStyle);
            createCell(row, 2, staff.getFullName(), bodyStyle);
            createCell(row, 3, staff.getGradeLevel() != null ? staff.getGradeLevel().getName() : "", bodyStyle);

            Map<Long, Map<Long, List<String>>> staffAssignments = grouped.getOrDefault(staff.getId(), Map.of());
            createCell(row, 4, formatAssignmentCell(staffAssignments.get(context.semesterOne() != null ? context.semesterOne().getId() : null), context.subjects()), bodyStyle);
            createCell(row, 5, formatAssignmentCell(staffAssignments.get(context.semesterTwo() != null ? context.semesterTwo().getId() : null), context.subjects()), bodyStyle);
        }

        sheet.setColumnWidth(0, 8 * 256);
        sheet.setColumnWidth(1, 20 * 256);
        sheet.setColumnWidth(2, 28 * 256);
        sheet.setColumnWidth(3, 18 * 256);
        sheet.setColumnWidth(4, 48 * 256);
        sheet.setColumnWidth(5, 48 * 256);
    }

    private void buildSubjectSheet(Sheet sheet, Workbook workbook, List<Subject> subjects) {
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle bodyStyle = createBodyStyle(workbook);

        Row headerRow = sheet.createRow(0);
        createCell(headerRow, 0, "STT", headerStyle);
        createCell(headerRow, 1, "Ký hiệu môn học", headerStyle);
        createCell(headerRow, 2, "Tên môn học", headerStyle);

        int rowIndex = 1;
        int stt = 1;
        for (Subject subject : subjects) {
            Row row = sheet.createRow(rowIndex++);
            createCell(row, 0, stt++, bodyStyle);
            createCell(row, 1, subject.getCode(), bodyStyle);
            createCell(row, 2, subject.getName(), bodyStyle);
        }

        sheet.setColumnWidth(0, 8 * 256);
        sheet.setColumnWidth(1, 20 * 256);
        sheet.setColumnWidth(2, 40 * 256);
    }

    private List<TeacherAssignment> readAssignmentsFromExcel(MultipartFile file, ExportContext context) {
        try (InputStream inputStream = file.getInputStream(); Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheet("PCGD");
            if (sheet == null) {
                sheet = workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null;
            }
            if (sheet == null) {
                throw new UserMessageException("File Excel không có sheet PCGD");
            }

            DataFormatter formatter = new DataFormatter();
            Map<String, Staff> staffByCode = context.staffs().stream()
                    .collect(Collectors.toMap(item -> normalize(item.getStaffCode()), item -> item, (left, right) -> left));

            List<TeacherAssignment> results = new ArrayList<>();
            Set<String> teacherSemesterClassKeys = new LinkedHashSet<>();

            for (int rowIndex = DATA_START_ROW_INDEX; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) {
                    continue;
                }

                String staffCode = readCellText(row.getCell(1), formatter);
                if (!StringUtils.hasText(staffCode)) {
                    continue;
                }

                Staff staff = staffByCode.get(normalize(staffCode));
                if (staff == null) {
                    throw new UserMessageException("Không tìm thấy giáo viên với mã: " + staffCode);
                }

                parseAssignmentCell(readCellText(row.getCell(4), formatter), staff, context.semesterOne(), context, results,
                        teacherSemesterClassKeys);
                parseAssignmentCell(readCellText(row.getCell(5), formatter), staff, context.semesterTwo(), context, results,
                        teacherSemesterClassKeys);
            }

            return results;
        } catch (IOException ex) {
            throw new UserMessageException("Không thể đọc file Excel phân công giảng dạy");
        }
    }

    private void parseAssignmentCell(String rawCellValue, Staff staff, Semester semester, ExportContext context,
            List<TeacherAssignment> results, Set<String> teacherSemesterClassKeys) {
        if (semester == null || !StringUtils.hasText(rawCellValue)) {
            return;
        }

        for (String item : rawCellValue.split("\\+")) {
            String expression = item == null ? "" : item.trim();
            if (expression.isEmpty()) {
                continue;
            }

            Matcher matcher = ASSIGNMENT_PATTERN.matcher(expression);
            if (!matcher.matches()) {
                throw new UserMessageException("Sai định dạng phân công: " + expression);
            }

            String subjectToken = normalize(matcher.group(1));
            Subject subject = context.subjectByCodeOrName().get(subjectToken);
            if (subject == null) {
                throw new UserMessageException("Không tìm thấy môn học: " + matcher.group(1).trim());
            }

            String[] classTokens = matcher.group(2).split(",");
            for (String classToken : classTokens) {
                String className = classToken == null ? "" : classToken.trim();
                if (className.isEmpty()) {
                    continue;
                }

                Classroom classroom = context.classroomByName().get(normalize(className));
                if (classroom == null) {
                    throw new UserMessageException("Không tìm thấy lớp: " + className);
                }

                if (!context.validClassSubjectPairs().contains(buildPairKey(classroom.getId(), subject.getId()))) {
                    throw new UserMessageException(
                            "Môn " + subject.getCode() + " chưa được cấu hình cho lớp " + classroom.getName());
                }

                String duplicateKey = staff.getId() + "|" + semester.getId() + "|" + classroom.getId();
                if (!teacherSemesterClassKeys.add(duplicateKey)) {
                    throw new UserMessageException("Giáo viên " + staff.getStaffCode()
                            + " bị trùng lớp trong cùng học kỳ: " + classroom.getName());
                }

                TeacherAssignment assignment = new TeacherAssignment();
                assignment.setStaff(staff);
                assignment.setSchoolYear(context.schoolYear());
                assignment.setSemester(semester);
                assignment.setClassroom(classroom);
                assignment.setSubject(subject);
                results.add(assignment);
            }
        }
    }

    private String formatAssignmentCell(Map<Long, List<String>> assignmentBySubjectId, List<Subject> subjects) {
        if (assignmentBySubjectId == null || assignmentBySubjectId.isEmpty()) {
            return "";
        }

        Map<Long, Subject> subjectMap = subjects.stream()
                .collect(Collectors.toMap(Subject::getId, item -> item, (left, right) -> left));

        return assignmentBySubjectId.entrySet().stream()
                .map(entry -> {
                    Subject subject = subjectMap.get(entry.getKey());
                    if (subject == null) {
                        return null;
                    }
                    String classNames = entry.getValue().stream().filter(StringUtils::hasText).distinct()
                            .collect(Collectors.joining(", "));
                    return subject.getCode() + "(" + classNames + ")";
                })
                .filter(Objects::nonNull)
                .collect(Collectors.joining("+"));
    }

    private Subject findSubjectOrThrow(Long subjectId) {
        return subjectRepository.findById(subjectId)
                .orElseThrow(() -> new UserMessageException(CommonErrorCode.SUBJECT_NOT_FOUND));
    }

    private Classroom findClassroomOrThrow(Long classroomId) {
        return classroomRepository.findById(classroomId)
                .orElseThrow(() -> new UserMessageException(CommonErrorCode.CLASS_NOT_FOUND));
    }

    private Semester findSemesterOrThrow(Long semesterId) {
        return semesterRepository.findById(semesterId)
                .filter(item -> item.getDeletedFlag() == 0)
                .orElseThrow(() -> new UserMessageException(CommonErrorCode.SEMESTER_NOT_FOUND));
    }

    private void validateClassroomContext(Classroom classroom, Long unitId, Long schoolYearId) {
        if (!classroom.getUnit().getId().equals(unitId)) {
            throw new UserMessageException("Lớp " + classroom.getId() + " không thuộc đơn vị đã chọn");
        }
        if (!classroom.getSchoolYear().getId().equals(schoolYearId)) {
            throw new UserMessageException("Lớp " + classroom.getId() + " không thuộc năm học đã chọn");
        }
    }

    private void validateSemesterBelongsToSchoolYear(Semester semester, Long schoolYearId) {
        if (!semester.getSchoolYear().getId().equals(schoolYearId)) {
            throw new UserMessageException(CommonErrorCode.WEEK_CONFIG_SEMESTER_SCHOOL_YEAR_MISMATCH);
        }
    }

    private void validateExcelFile(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getOriginalFilename() == null
                || !file.getOriginalFilename().toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
            throw new UserMessageException("File import phải là file Excel .xlsx");
        }
    }

    private String buildSemesterHeader(Semester semester, String fallback) {
        return semester == null ? fallback : "Phân công giảng dạy " + semester.getName();
    }

    private String readCellText(Cell cell, DataFormatter formatter) {
        return cell == null ? "" : formatter.formatCellValue(cell).trim();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private Long buildPairKey(Long classroomId, Long subjectId) {
        return classroomId * 1_000_000L + subjectId;
    }

    private Cell createCell(Row row, int columnIndex, Object value, CellStyle style) {
        Cell cell = row.getCell(columnIndex);
        if (cell == null) {
            cell = row.createCell(columnIndex);
        }
        if (value == null) {
            cell.setBlank();
        } else if (value instanceof Number number) {
            cell.setCellValue(number.doubleValue());
        } else {
            cell.setCellValue(String.valueOf(value));
        }
        if (style != null) {
            cell.setCellStyle(style);
        }
        return cell;
    }

    private CellStyle createTitleStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 14);
        style.setFont(font);
        return style;
    }

    private CellStyle createGuideLabelStyle(Workbook workbook) {
        CellStyle style = createBodyStyle(workbook);
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private CellStyle createGuideContentStyle(Workbook workbook) {
        CellStyle style = createBodyStyle(workbook);
        style.setWrapText(true);
        return style;
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setFillForegroundColor((short) 44);
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    private CellStyle createBodyStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setVerticalAlignment(VerticalAlignment.TOP);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setWrapText(true);
        return style;
    }

    private int normalizePageSize(Integer pageSize) {
        return pageSize == null || pageSize <= 0 ? 20 : pageSize;
    }

    private int normalizePageNow(Integer pageNow) {
        return pageNow == null || pageNow <= 0 ? 1 : pageNow;
    }

    private record AssignmentSeed(Classroom classroom, Subject subject) {
    }

    private record AssignmentBuildContext(Staff staff, SchoolYear schoolYear, Semester semester,
            List<AssignmentSeed> seeds, List<Long> classroomIds) {
    }

    private record ExportContext(SchoolYear schoolYear, Unit unit, Semester semesterOne, Semester semesterTwo,
            List<Staff> staffs, List<Classroom> classrooms, List<Subject> subjects,
            Map<String, Classroom> classroomByName, Map<String, Subject> subjectByCodeOrName,
            Set<Long> validClassSubjectPairs, List<TeacherAssignment> existingAssignments) {
    }
}
