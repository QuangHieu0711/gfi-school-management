package com.gfi.backend.services.implement;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.nio.file.Files;
import java.util.Set;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.gfi.backend.controllers.exceptions.UserMessageException;
import com.gfi.backend.models.dtos.common.LookupItemDto;
import com.gfi.backend.models.dtos.common.PageRequestDto;
import com.gfi.backend.models.dtos.common.PageResponseDto;
import com.gfi.backend.models.dtos.common.TemporaryFileDto;
import com.gfi.backend.models.dtos.student.StudentAddressCreateRequest;
import com.gfi.backend.models.dtos.student.StudentAddressItemDto;
import com.gfi.backend.models.dtos.student.StudentCreateRequest;
import com.gfi.backend.models.dtos.student.StudentEnrollmentCreateRequest;
import com.gfi.backend.models.dtos.student.StudentEnrollmentItemDto;
import com.gfi.backend.models.dtos.student.StudentFilterDto;
import com.gfi.backend.models.dtos.student.StudentGuardianCreateRequest;
import com.gfi.backend.models.dtos.student.StudentGuardianItemDto;
import com.gfi.backend.models.dtos.student.StudentImportResultDto;
import com.gfi.backend.models.dtos.student.StudentItemDto;
import com.gfi.backend.models.dtos.student.StudentReportCardExportRequest;
import com.gfi.backend.models.dtos.student.StudentTransferClassRequest;
import com.gfi.backend.models.dtos.student.StudentTransferClassResultDto;
import com.gfi.backend.models.dtos.student.StudentProfileCreateRequest;
import com.gfi.backend.models.dtos.student.StudentProfileItemDto;
import com.gfi.backend.models.entities.Classroom;
import com.gfi.backend.models.entities.ClassroomSubject;
import com.gfi.backend.models.entities.AttendanceRecord;
import com.gfi.backend.models.entities.SchoolYear;
import com.gfi.backend.models.entities.Student;
import com.gfi.backend.models.entities.StudentAddress;
import com.gfi.backend.models.entities.StudentEnrollment;
import com.gfi.backend.models.entities.StudentEvaluation;
import com.gfi.backend.models.entities.StudentGuardian;
import com.gfi.backend.models.entities.StudentProfile;
import com.gfi.backend.models.entities.Unit;
import com.gfi.backend.models.enums.ActionType;
import com.gfi.backend.models.enums.ExportType;
import com.gfi.backend.models.enums.ScopeType;
import com.gfi.backend.models.global.CommonErrorCode;
import com.gfi.backend.models.security.FeatureKey;
import com.gfi.backend.models.security.ResolvedScope;
import com.gfi.backend.repositories.ClassroomRepository;
import com.gfi.backend.repositories.ClassroomSubjectRepository;
import com.gfi.backend.repositories.SchoolYearRepository;
import com.gfi.backend.repositories.AttendanceRecordRepository;
import com.gfi.backend.repositories.StudentAddressRepository;
import com.gfi.backend.repositories.StudentEnrollmentRepository;
import com.gfi.backend.repositories.StudentEvaluationRepository;
import com.gfi.backend.repositories.StudentGuardianRepository;
import com.gfi.backend.repositories.StudentProfileRepository;
import com.gfi.backend.repositories.StudentRepository;
import com.gfi.backend.repositories.UnitRepository;
import com.gfi.backend.services.FileStorageService;
import com.gfi.backend.services.interfaces.DataScopeFilterService;
import com.gfi.backend.services.interfaces.ImportErrorFileStorageService;
import com.gfi.backend.services.interfaces.StudentCodeGeneratorService;
import com.gfi.backend.services.interfaces.StudentService;
import com.gfi.backend.utils.PageableUtils;
import com.gfi.backend.utils.SecurityUtils;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StudentServiceImpl implements StudentService {

    private static final String ADDRESS_TYPE_PERMANENT = "PERMANENT";
    private static final String GUARDIAN_TYPE_FATHER = "FATHER";
    private static final String GUARDIAN_TYPE_MOTHER = "MOTHER";
    private static final String GUARDIAN_TYPE_GUARDIAN = "GUARDIAN";
    private static final String FEATURE = FeatureKey.STUDENT_PROFILE.getCode();
    private static final String EXPORT_FONT_NAME = "Times New Roman";
    private static final String EXCEL_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter EXPORT_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final String TIMES_FONT_REGULAR_PATH = "C:/Windows/Fonts/times.ttf";
    private static final String TIMES_FONT_BOLD_PATH = "C:/Windows/Fonts/timesbd.ttf";
    private static final String TIMES_FONT_ITALIC_PATH = "C:/Windows/Fonts/timesi.ttf";
    private static final List<String> REPORT_CARD_SUBJECT_TEMPLATE = List.of(
            "Tiếng Việt",
            "Toán",
            "Ngoại ngữ 1",
            "Lịch sử và Địa lý",
            "Khoa học",
            "Tin học và Công nghệ (Tin học)",
            "Tin học và Công nghệ (Công nghệ)",
            "Đạo đức",
            "Tự nhiên và Xã hội",
            "Giáo dục thể chất",
            "Nghệ thuật (Âm nhạc)",
            "Nghệ thuật (Mĩ thuật)",
            "Hoạt động trải nghiệm",
            "Tiếng dân tộc");
    private static final int STUDENT_TEMPLATE_GROUP_HEADER_ROW_INDEX = 7;
    private static final int STUDENT_IMPORT_HEADER_ROW_INDEX = 8;
    private static final int STUDENT_IMPORT_DATA_START_ROW_INDEX = 9;
    private static final int STUDENT_COL_FULL_NAME = 1;
    private static final int STUDENT_COL_FIRST_NAME = 2;
    private static final int STUDENT_COL_DOB = 3;
    private static final int STUDENT_COL_GENDER = 4;
    private static final int STUDENT_COL_PHONE = 5;
    private static final int STUDENT_COL_EMAIL = 6;
    private static final int STUDENT_COL_SCHOOL_YEAR_ID = 7;
    private static final int STUDENT_COL_CLASS_ID = 8;
    private static final int STUDENT_COL_ADMISSION_DATE = 9;
    private static final int STUDENT_COL_STUDENT_STATUS = 10;
    private static final int STUDENT_COL_MOE_CODE = 11;
    private static final int STUDENT_COL_PLACE_OF_BIRTH = 12;
    private static final int STUDENT_COL_ETHNICITY = 13;
    private static final int STUDENT_COL_RELIGION = 14;
    private static final int STUDENT_COL_NATIONALITY = 15;
    private static final int STUDENT_COL_IDENTITY_NUMBER = 16;
    private static final int STUDENT_COL_IDENTITY_ISSUE_DATE = 17;
    private static final int STUDENT_COL_IDENTITY_ISSUE_PLACE = 18;
    private static final int STUDENT_COL_HEALTH_INSURANCE_NO = 19;
    private static final int STUDENT_COL_BLOOD_GROUP = 20;
    private static final int STUDENT_COL_BOARDING_BOOK = 21;
    private static final int STUDENT_COL_ADMISSION_TYPE = 22;
    private static final int STUDENT_COL_ENROLLMENT_STATUS = 23;
    private static final int STUDENT_COL_ENROLLMENT_IS_REPEATER = 24;
    private static final int STUDENT_COL_ENROLLMENT_SESSIONS = 25;
    private static final int STUDENT_COL_ENROLLMENT_STUDY_MODE = 26;
    private static final int STUDENT_COL_ENROLLMENT_BOARDING = 27;
    private static final int STUDENT_COL_ENROLLMENT_TWO_SESSIONS = 28;
    private static final int STUDENT_COL_ADDR_PROVINCE = 29;
    private static final int STUDENT_COL_ADDR_WARD = 30;
    private static final int STUDENT_COL_ADDR_HAMLET = 31;
    private static final int STUDENT_COL_ADDR_DETAIL = 32;
    private static final int STUDENT_COL_FATHER_NAME = 33;
    private static final int STUDENT_COL_FATHER_BIRTH_YEAR = 34;
    private static final int STUDENT_COL_FATHER_OCCUPATION = 35;
    private static final int STUDENT_COL_FATHER_PHONE = 36;
    private static final int STUDENT_COL_FATHER_EMAIL = 37;
    private static final int STUDENT_COL_FATHER_IDENTITY = 38;
    private static final int STUDENT_COL_FATHER_IS_ETHNIC = 39;
    private static final int STUDENT_COL_MOTHER_NAME = 40;
    private static final int STUDENT_COL_MOTHER_BIRTH_YEAR = 41;
    private static final int STUDENT_COL_MOTHER_OCCUPATION = 42;
    private static final int STUDENT_COL_MOTHER_PHONE = 43;
    private static final int STUDENT_COL_MOTHER_EMAIL = 44;
    private static final int STUDENT_COL_MOTHER_IDENTITY = 45;
    private static final int STUDENT_COL_MOTHER_IS_ETHNIC = 46;
    private static final int STUDENT_COL_PROFILE_POLICY_OBJECT = 47;
    private static final int STUDENT_COL_PROFILE_POLICY_BENEFIT = 48;
    private static final int STUDENT_COL_PROFILE_PRIORITY_CATEGORY = 49;
    private static final int STUDENT_COL_PROFILE_REGION_CATEGORY = 50;
    private static final int STUDENT_COL_PROFILE_DISABILITY_TYPE = 51;
    private static final int STUDENT_COL_PROFILE_DISABILITY_EXEMPT = 52;
    private static final int STUDENT_COL_PROFILE_SUPPORT_TUITION = 53;
    private static final int STUDENT_COL_PROFILE_PARENT_INTERNET = 54;
    private static final int STUDENT_COL_PROFILE_PARENT_SMARTPHONE = 55;
    private static final int STUDENT_COL_PROFILE_OTHER_SYSTEM_CODE = 56;
    private static final int STUDENT_COL_PROFILE_SSO_CODE = 57;
    private static final int STUDENT_IMPORT_LAST_DATA_COLUMN = STUDENT_COL_PROFILE_SSO_CODE;

    private final StudentRepository studentRepository;
    private final StudentEnrollmentRepository studentEnrollmentRepository;
    private final StudentAddressRepository studentAddressRepository;
    private final StudentGuardianRepository studentGuardianRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final UnitRepository unitRepository;
    private final SchoolYearRepository schoolYearRepository;
    private final ClassroomRepository classroomRepository;
    private final FileStorageService fileStorageService;
    private final DataScopeFilterService dataScopeFilterService;
    private final StudentCodeGeneratorService studentCodeGeneratorService;
    private final ImportErrorFileStorageService importErrorFileStorageService;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final StudentEvaluationRepository studentEvaluationRepository;
    private final ClassroomSubjectRepository classroomSubjectRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResponseDto<StudentItemDto, StudentFilterDto> search(PageRequestDto<StudentFilterDto> request) {
        StudentFilterDto filter = request.getFilter() == null ? new StudentFilterDto() : request.getFilter();
        int pageSize = normalizePageSize(request.getPageSize());
        int pageNow = normalizePageNow(request.getPageNow());
        Pageable pageable = PageableUtils.newestFirst(pageNow, pageSize);
        List<ResolvedScope> resolvedScopes = dataScopeFilterService.getResolvedScopes(FEATURE, ActionType.VIEW);

        Page<Student> page = studentRepository.findAll(buildSpecification(filter, resolvedScopes), pageable);
        List<StudentItemDto> items = page.getContent().stream().map(this::toDto).toList();

        return PageResponseDto.<StudentItemDto, StudentFilterDto>builder()
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
    public List<LookupItemDto> getStudentsByClassroom(Long classroomId) {
        findClassroom(classroomId);
        return studentEnrollmentRepository.findByClassroomIdAndDeletedFlagOrderByStudentFullNameAsc(classroomId, 0)
                .stream()
                .map(StudentEnrollment::getStudent)
                .map(student -> LookupItemDto.builder()
                        .id(student.getId())
                        .code(student.getStudentCode())
                        .name(student.getFullName())
                        .build())
                .toList();
    }

    @Override
    @Transactional
    public StudentItemDto create(StudentCreateRequest request) {
        Unit unit = findUnit(request.getUnitId());
        SchoolYear schoolYear = findSchoolYear(request.getEnrollment().getSchoolYearId());
        Classroom classroom = findClassroom(request.getEnrollment().getClassId());
        validateStudentScope(ActionType.ADD, unit, classroom, null);

        // Sinh mã học sinh tự động nếu không cung cấp
        String studentCode;
        String providedCode = normalize(request.getStudentCode());
        if (providedCode == null || providedCode.isEmpty()) {
            // Sinh mã tự động theo format: HS-{UNIT_CODE}-{YEAR}-{STT}
            Integer year = schoolYear.getStartDate().getYear();
            studentCode = studentCodeGeneratorService.generateStudentCode(unit.getId(), year);
        } else {
            studentCode = providedCode;
            // Kiểm tra mã không được trùng
            studentRepository.findByStudentCode(studentCode)
                    .ifPresent(item -> {
                        throw new UserMessageException(CommonErrorCode.STUDENT_CODE_ALREADY_EXISTS);
                    });
        }

        validateEnrollment(unit, schoolYear, classroom);
        validateAddressTypes(request.getAddresses());
        validateGuardianTypes(request.getGuardians());

        Student student = new Student();
        applyStudentFields(student, request, unit);
        student.setStudentCode(studentCode);

        // Xử lý avatarUrl nếu có
        if (request.getAvatarUrl() != null && !request.getAvatarUrl().isBlank()) {
            String avatarUrl = fileStorageService.storeStudentAvatarFromDataUrl(
                    request.getAvatarUrl(),
                    unit.getName(),
                    schoolYear.getName());
            student.setAvatarUrl(avatarUrl);
        }

        student.setCreatedBy(getCurrentUsername());
        Student savedStudent = studentRepository.save(student);

        StudentEnrollment savedEnrollment = saveOrUpdateEnrollment(savedStudent, schoolYear, classroom,
                request.getEnrollment());
        List<StudentAddress> savedAddresses = replaceAddresses(savedStudent, request.getAddresses());
        List<StudentGuardian> savedGuardians = replaceGuardians(savedStudent, request.getGuardians());
        StudentProfile savedProfile = replaceProfile(savedStudent, request.getProfile());

        return toDto(savedStudent, savedEnrollment, savedAddresses, savedGuardians, savedProfile);
    }

    @Override
    @Transactional(readOnly = true)
    public StudentItemDto getById(Long id) {
        Student student = findStudent(id);
        validateStudentScope(ActionType.VIEW, student);
        return toDto(student);
    }

    @Override
    @Transactional
    public StudentItemDto update(Long id, StudentCreateRequest request) {
        Student student = findStudent(id);
        validateStudentScope(ActionType.EDIT, student);

        // Khi update: giữ nguyên mã cũ, không cho phép thay đổi
        // Kiểm tra nếu request cố gắng thay đổi mã thì báo lỗi
        String providedCode = normalize(request.getStudentCode());
        if (providedCode != null && !providedCode.isEmpty() && !providedCode.equals(student.getStudentCode())) {
            throw new UserMessageException(CommonErrorCode.STUDENT_CODE_ALREADY_EXISTS);
        }

        Unit unit = findUnit(request.getUnitId());
        SchoolYear schoolYear = findSchoolYear(request.getEnrollment().getSchoolYearId());
        Classroom classroom = findClassroom(request.getEnrollment().getClassId());
        validateStudentScope(ActionType.EDIT, unit, classroom, null);

        validateEnrollment(unit, schoolYear, classroom);
        validateAddressTypes(request.getAddresses());
        validateGuardianTypes(request.getGuardians());

        applyStudentFields(student, request, unit);

        // Xử lý avatarUrl nếu có
        if (request.getAvatarUrl() != null && !request.getAvatarUrl().isBlank()) {
            String avatarUrl = fileStorageService.storeStudentAvatarFromDataUrl(
                    request.getAvatarUrl(),
                    unit.getName(),
                    schoolYear.getName());
            student.setAvatarUrl(avatarUrl);
        }

        student.setUpdatedBy(getCurrentUsername());
        Student savedStudent = studentRepository.save(student);

        StudentEnrollment savedEnrollment = saveOrUpdateEnrollment(savedStudent, schoolYear, classroom,
                request.getEnrollment());
        List<StudentAddress> savedAddresses = replaceAddresses(savedStudent, request.getAddresses());
        List<StudentGuardian> savedGuardians = replaceGuardians(savedStudent, request.getGuardians());
        StudentProfile savedProfile = replaceProfile(savedStudent, request.getProfile());

        return toDto(savedStudent, savedEnrollment, savedAddresses, savedGuardians, savedProfile);
    }

    @Override
    @Transactional
    public StudentTransferClassResultDto transferClass(StudentTransferClassRequest request) {
        List<Long> uniqueStudentIds = normalizeStudentIds(request.getStudentIds());
        SchoolYear targetSchoolYear = findSchoolYear(request.getTargetSchoolYearId());
        Classroom targetClassroom = findClassroom(request.getTargetClassId());
        validateTransferTarget(targetSchoolYear, targetClassroom);

        for (Long studentId : uniqueStudentIds) {
            Student student = findStudent(studentId);
            validateStudentScope(ActionType.EDIT, student);
            transferStudentToClass(student, targetSchoolYear, targetClassroom, request);
        }

        return StudentTransferClassResultDto.builder()
                .transferredCount(uniqueStudentIds.size())
                .targetSchoolYearId(targetSchoolYear.getId())
                .targetSchoolYearName(targetSchoolYear.getName())
                .targetClassId(targetClassroom.getId())
                .targetClassName(targetClassroom.getName())
                .isRepeater(request.getIsRepeater())
                .studentIds(uniqueStudentIds)
                .build();
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Student student = findStudent(id);
        validateStudentScope(ActionType.DELETE, student);
        studentProfileRepository.deleteByStudentId(id);
        studentGuardianRepository.deleteByStudentId(id);
        studentAddressRepository.deleteByStudentId(id);
        studentEnrollmentRepository.deleteByStudentId(id);
        studentRepository.delete(student);
    }

    private Specification<Student> buildSpecification(StudentFilterDto filter, List<ResolvedScope> resolvedScopes) {
        return (root, query, cb) -> {
            query.distinct(true);
            List<Predicate> predicates = new java.util.ArrayList<>();
            Join<Student, Unit> unitJoin = root.join("unit", JoinType.LEFT);

            if (filter.getUnitId() != null) {
                predicates.add(cb.equal(unitJoin.get("id"), filter.getUnitId()));
            }
            if (hasText(filter.getFullName())) {
                predicates.add(cb.like(cb.lower(root.get("fullName")), likeValue(filter.getFullName())));
            }
            if (hasText(filter.getFirstName())) {
                predicates.add(cb.like(cb.lower(root.get("firstName")), likeValue(filter.getFirstName())));
            }
            if (filter.getStudentStatus() != null) {
                predicates.add(cb.equal(root.get("studentStatus"), filter.getStudentStatus()));
            }
            if (hasText(filter.getMoeCode())) {
                predicates.add(cb.like(cb.lower(root.get("moeCode")), likeValue(filter.getMoeCode())));
            }
            if (filter.getDateOfBirth() != null) {
                predicates.add(cb.equal(root.get("dateOfBirth"), filter.getDateOfBirth()));
            }
            if (filter.getGender() != null) {
                predicates.add(cb.equal(root.get("gender"), filter.getGender()));
            }
            if (hasText(filter.getStudentCode())) {
                predicates.add(cb.like(cb.lower(root.get("studentCode")), likeValue(filter.getStudentCode())));
            }

            if (filter.getClassId() != null || filter.getGradeLevelId() != null) {
                Subquery<Long> enrollmentSubquery = query.subquery(Long.class);
                Root<StudentEnrollment> enrollmentRoot = enrollmentSubquery.from(StudentEnrollment.class);
                Join<StudentEnrollment, Classroom> classroomJoin = enrollmentRoot.join("classroom", JoinType.INNER);
                List<Predicate> enrollmentPredicates = new java.util.ArrayList<>();
                enrollmentPredicates.add(cb.equal(enrollmentRoot.get("student").get("id"), root.get("id")));
                if (filter.getClassId() != null) {
                    enrollmentPredicates.add(cb.equal(classroomJoin.get("id"), filter.getClassId()));
                }
                if (filter.getGradeLevelId() != null) {
                    enrollmentPredicates
                            .add(cb.equal(classroomJoin.get("gradeLevel").get("id"), filter.getGradeLevelId()));
                }
                enrollmentSubquery.select(enrollmentRoot.get("id"))
                        .where(cb.and(enrollmentPredicates.toArray(new Predicate[0])));
                predicates.add(cb.exists(enrollmentSubquery));
            }

            if (hasText(filter.getOtherSystemCode())) {
                Subquery<Long> profileSubquery = query.subquery(Long.class);
                Root<StudentProfile> profileRoot = profileSubquery.from(StudentProfile.class);
                profileSubquery.select(profileRoot.get("id"))
                        .where(
                                cb.equal(profileRoot.get("student").get("id"), root.get("id")),
                                cb.like(cb.lower(profileRoot.get("otherSystemCode")),
                                        likeValue(filter.getOtherSystemCode())));
                predicates.add(cb.exists(profileSubquery));
            }

            if (hasText(filter.getFatherPhone())) {
                predicates.add(buildGuardianPhonePredicate(query.subquery(Long.class), cb, root, GUARDIAN_TYPE_FATHER,
                        filter.getFatherPhone()));
            }
            if (hasText(filter.getMotherPhone())) {
                predicates.add(buildGuardianPhonePredicate(query.subquery(Long.class), cb, root, GUARDIAN_TYPE_MOTHER,
                        filter.getMotherPhone()));
            }

            if (hasText(filter.getPermanentProvinceName()) || hasText(filter.getPermanentWardName())) {
                Subquery<Long> addressSubquery = query.subquery(Long.class);
                Root<StudentAddress> addressRoot = addressSubquery.from(StudentAddress.class);
                List<Predicate> addressPredicates = new java.util.ArrayList<>();
                addressPredicates.add(cb.equal(addressRoot.get("student").get("id"), root.get("id")));
                addressPredicates.add(cb.equal(addressRoot.get("addressType"), ADDRESS_TYPE_PERMANENT));
                if (hasText(filter.getPermanentProvinceName())) {
                    addressPredicates.add(
                            cb.like(cb.lower(addressRoot.get("provinceName")),
                                    likeValue(filter.getPermanentProvinceName())));
                }
                if (hasText(filter.getPermanentWardName())) {
                    addressPredicates
                            .add(cb.like(cb.lower(addressRoot.get("wardName")),
                                    likeValue(filter.getPermanentWardName())));
                }
                addressSubquery.select(addressRoot.get("id"))
                        .where(cb.and(addressPredicates.toArray(new Predicate[0])));
                predicates.add(cb.exists(addressSubquery));
            }

            predicates.add(buildStudentScopePredicate(query, cb, root, unitJoin, resolvedScopes));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Predicate buildGuardianPhonePredicate(Subquery<Long> subquery,
            jakarta.persistence.criteria.CriteriaBuilder cb,
            Root<Student> root, String guardianType, String phone) {
        Root<StudentGuardian> guardianRoot = subquery.from(StudentGuardian.class);
        subquery.select(guardianRoot.get("id"))
                .where(
                        cb.equal(guardianRoot.get("student").get("id"), root.get("id")),
                        cb.equal(guardianRoot.get("guardianType"), guardianType),
                        cb.like(cb.lower(guardianRoot.get("phone")), likeValue(phone)));
        return cb.exists(subquery);
    }

    private Student findStudent(Long id) {
        return studentRepository.findById(id)
                .orElseThrow(() -> new UserMessageException(CommonErrorCode.STUDENT_NOT_FOUND));
    }

    private void validateStudentScope(ActionType action, Student student) {
        StudentEnrollment enrollment = findLatestEnrollment(student.getId());
        Classroom classroom = enrollment == null ? null : enrollment.getClassroom();
        validateStudentScope(action, student.getUnit(), classroom, student.getId());
    }

    private void validateStudentScope(ActionType action, Unit unit, Classroom classroom, Long studentId) {
        List<ResolvedScope> resolvedScopes = dataScopeFilterService.getResolvedScopes(FEATURE, action);
        if (!hasStudentScope(resolvedScopes, unit, classroom, studentId)) {
            throw new AccessDeniedException(
                    "User khong co quyen " + action + " tren student trong scope hien tai");
        }
    }

    private boolean hasStudentScope(List<ResolvedScope> resolvedScopes, Unit unit, Classroom classroom,
            Long studentId) {
        if (resolvedScopes == null || resolvedScopes.isEmpty()) {
            return false;
        }
        for (ResolvedScope scope : resolvedScopes) {
            if (scope == null) {
                continue;
            }
            if (scope.isUnrestricted() || scope.getScopeType() == ScopeType.ALL) {
                return true;
            }
            if (scope.getScopeIds() == null || scope.getScopeIds().isEmpty()) {
                continue;
            }
            switch (scope.getScopeType()) {
                case UNIT -> {
                    if (unit != null && scope.getScopeIds().contains(unit.getId())) {
                        return true;
                    }
                }
                case CLASS -> {
                    if (classroom != null && scope.getScopeIds().contains(classroom.getId())) {
                        return true;
                    }
                }
                case GRADE -> {
                    if (classroom != null && classroom.getGradeLevel() != null
                            && scope.getScopeIds().contains(classroom.getGradeLevel().getId())) {
                        return true;
                    }
                }
                case USER, SELF -> {
                }
                default -> {
                }
            }
        }
        return false;
    }

    private Predicate buildStudentScopePredicate(jakarta.persistence.criteria.CriteriaQuery<?> query,
            jakarta.persistence.criteria.CriteriaBuilder cb,
            Root<Student> root,
            Join<Student, Unit> unitJoin,
            List<ResolvedScope> resolvedScopes) {
        if (resolvedScopes == null || resolvedScopes.isEmpty()) {
            return cb.disjunction();
        }
        List<Predicate> scopePredicates = new java.util.ArrayList<>();
        for (ResolvedScope scope : resolvedScopes) {
            if (scope == null) {
                continue;
            }
            if (scope.isUnrestricted() || scope.getScopeType() == ScopeType.ALL) {
                return cb.conjunction();
            }
            if (scope.getScopeIds() == null || scope.getScopeIds().isEmpty()) {
                continue;
            }
            switch (scope.getScopeType()) {
                case UNIT -> scopePredicates.add(unitJoin.get("id").in(scope.getScopeIds()));
                case CLASS -> scopePredicates
                        .add(buildEnrollmentScopeExistsSubquery(query, cb, root, scope.getScopeIds(), false));
                case GRADE ->
                    scopePredicates.add(buildEnrollmentScopeExistsSubquery(query, cb, root, scope.getScopeIds(), true));
                case USER, SELF -> {
                }
                default -> {
                }
            }
        }
        return scopePredicates.isEmpty() ? cb.disjunction() : cb.or(scopePredicates.toArray(new Predicate[0]));
    }

    private Predicate buildEnrollmentScopeExistsSubquery(jakarta.persistence.criteria.CriteriaQuery<?> query,
            jakarta.persistence.criteria.CriteriaBuilder cb,
            Root<Student> root,
            Set<Long> scopeIds,
            boolean byGrade) {
        Subquery<Long> subquery = query.subquery(Long.class);
        Root<StudentEnrollment> enrollmentRoot = subquery.from(StudentEnrollment.class);
        Join<StudentEnrollment, Classroom> classroomJoin = enrollmentRoot.join("classroom", JoinType.INNER);
        Path<?> scopePath = byGrade ? classroomJoin.get("gradeLevel").get("id") : classroomJoin.get("id");
        subquery.select(enrollmentRoot.get("id"))
                .where(
                        cb.equal(enrollmentRoot.get("student").get("id"), root.get("id")),
                        scopePath.in(scopeIds));
        return cb.exists(subquery);
    }

    private StudentEnrollment findLatestEnrollment(Long studentId) {
        return studentEnrollmentRepository.findByStudentIdOrderBySchoolYearIdDescIdDesc(studentId)
                .stream()
                .findFirst()
                .orElse(null);
    }

    private Unit findUnit(Long id) {
        return unitRepository.findById(id)
                .orElseThrow(() -> new UserMessageException(CommonErrorCode.UNIT_NOT_FOUND));
    }

    private SchoolYear findSchoolYear(Long id) {
        return schoolYearRepository.findById(id)
                .orElseThrow(() -> new UserMessageException(CommonErrorCode.SCHOOL_YEAR_NOT_FOUND));
    }

    private SchoolYear findSchoolYearByName(String name) {
        String normalizedName = normalizeNullable(name);
        if (normalizedName == null) {
            throw new UserMessageException("Năm học không hợp lệ");
        }
        return schoolYearRepository.findByName(normalizedName)
                .orElseThrow(() -> new UserMessageException("Năm học không hợp lệ: " + normalizedName));
    }

    private Classroom findClassroom(Long id) {
        return classroomRepository.findById(id)
                .orElseThrow(() -> new UserMessageException(CommonErrorCode.CLASS_NOT_FOUND));
    }

    private Classroom findClassroomByName(Long unitId, Long schoolYearId, String name) {
        String normalizedName = normalizeNullable(name);
        if (normalizedName == null) {
            throw new UserMessageException("Lớp không hợp lệ");
        }
        return classroomRepository.findByUnitIdAndSchoolYearIdAndName(unitId, schoolYearId, normalizedName)
                .orElseThrow(() -> new UserMessageException("Lớp không hợp lệ: " + normalizedName));
    }

    private void validateEnrollment(Unit unit, SchoolYear schoolYear, Classroom classroom) {
        if (classroom.getSchoolYear() == null || !schoolYear.getId().equals(classroom.getSchoolYear().getId())) {
            throw new UserMessageException(CommonErrorCode.STUDENT_ENROLLMENT_SCHOOL_YEAR_MISMATCH);
        }
        if (classroom.getUnit() == null || !unit.getId().equals(classroom.getUnit().getId())) {
            throw new UserMessageException(CommonErrorCode.STUDENT_ENROLLMENT_UNIT_MISMATCH);
        }
    }

    private List<Long> normalizeStudentIds(List<Long> studentIds) {
        LinkedHashSet<Long> normalized = new LinkedHashSet<>();
        for (Long studentId : safeList(studentIds)) {
            if (studentId != null) {
                normalized.add(studentId);
            }
        }
        if (normalized.isEmpty()) {
            throw new UserMessageException("Danh sach hoc sinh khong duoc de trong");
        }
        return List.copyOf(normalized);
    }

    private void validateTransferTarget(SchoolYear targetSchoolYear, Classroom targetClassroom) {
        if (targetClassroom.getSchoolYear() == null
                || !targetSchoolYear.getId().equals(targetClassroom.getSchoolYear().getId())) {
            throw new UserMessageException(CommonErrorCode.STUDENT_ENROLLMENT_SCHOOL_YEAR_MISMATCH);
        }
    }

    private void transferStudentToClass(Student student, SchoolYear targetSchoolYear, Classroom targetClassroom,
            StudentTransferClassRequest request) {
        validateEnrollment(student.getUnit(), targetSchoolYear, targetClassroom);

        StudentEnrollment sourceEnrollment = findLatestEnrollment(student.getId());
        StudentEnrollment targetEnrollment = studentEnrollmentRepository
                .findByStudentIdAndSchoolYearId(student.getId(), targetSchoolYear.getId())
                .orElseGet(StudentEnrollment::new);

        targetEnrollment.setStudent(student);
        targetEnrollment.setSchoolYear(targetSchoolYear);
        targetEnrollment.setClassroom(targetClassroom);
        targetEnrollment.setEnrolledAt(resolveTransferEnrolledAt(request, sourceEnrollment));
        targetEnrollment.setStatus(resolveTransferStatus(request, sourceEnrollment));
        targetEnrollment.setIsRepeater(resolveTransferIsRepeater(request));
        targetEnrollment.setSessionsPerWeek(resolveSessionsPerWeek(sourceEnrollment, targetEnrollment));
        targetEnrollment.setStudyMode(resolveStudyMode(sourceEnrollment, targetEnrollment));
        targetEnrollment.setIsBoarding(resolveBooleanFlag(
                sourceEnrollment == null ? null : sourceEnrollment.getIsBoarding(),
                targetEnrollment.getIsBoarding()));
        targetEnrollment.setIsTwoSessionsPerDay(resolveBooleanFlag(
                sourceEnrollment == null ? null : sourceEnrollment.getIsTwoSessionsPerDay(),
                targetEnrollment.getIsTwoSessionsPerDay()));

        if (targetEnrollment.getId() == null) {
            targetEnrollment.setCreatedBy(getCurrentUsername());
        } else {
            targetEnrollment.setUpdatedBy(getCurrentUsername());
        }

        studentEnrollmentRepository.save(targetEnrollment);
    }

    private java.time.LocalDate resolveTransferEnrolledAt(StudentTransferClassRequest request,
            StudentEnrollment sourceEnrollment) {
        if (request.getEnrolledAt() != null) {
            return request.getEnrolledAt();
        }
        return sourceEnrollment == null ? null : sourceEnrollment.getEnrolledAt();
    }

    private Integer resolveTransferStatus(StudentTransferClassRequest request, StudentEnrollment sourceEnrollment) {
        if (request.getStatus() != null) {
            return request.getStatus();
        }
        return sourceEnrollment == null ? 0 : sourceEnrollment.getStatus();
    }

    private Boolean resolveTransferIsRepeater(StudentTransferClassRequest request) {
        return Boolean.TRUE.equals(request.getIsRepeater());
    }

    private Integer resolveSessionsPerWeek(StudentEnrollment sourceEnrollment, StudentEnrollment targetEnrollment) {
        return sourceEnrollment != null && sourceEnrollment.getSessionsPerWeek() != null
                ? sourceEnrollment.getSessionsPerWeek()
                : targetEnrollment.getSessionsPerWeek();
    }

    private Integer resolveStudyMode(StudentEnrollment sourceEnrollment, StudentEnrollment targetEnrollment) {
        return sourceEnrollment != null && sourceEnrollment.getStudyMode() != null
                ? sourceEnrollment.getStudyMode()
                : targetEnrollment.getStudyMode();
    }

    private Boolean resolveBooleanFlag(Boolean sourceValue, Boolean currentTargetValue) {
        if (sourceValue != null) {
            return sourceValue;
        }
        return currentTargetValue != null ? currentTargetValue : Boolean.FALSE;
    }

    private void validateAddressTypes(List<StudentAddressCreateRequest> addresses) {
        Set<String> seen = new HashSet<>();
        for (StudentAddressCreateRequest address : safeList(addresses)) {
            String key = normalizeUpper(address.getAddressType());
            if (!seen.add(key)) {
                throw new UserMessageException(CommonErrorCode.STUDENT_ADDRESS_TYPE_DUPLICATED);
            }
        }
    }

    private void validateGuardianTypes(List<StudentGuardianCreateRequest> guardians) {
        Set<String> seen = new HashSet<>();
        for (StudentGuardianCreateRequest guardian : safeList(guardians)) {
            String key = normalizeUpper(guardian.getGuardianType());
            if (!seen.add(key)) {
                throw new UserMessageException(CommonErrorCode.STUDENT_GUARDIAN_TYPE_DUPLICATED);
            }
        }
    }

    private void applyStudentFields(Student student, StudentCreateRequest request, Unit unit) {
        student.setStudentCode(normalize(request.getStudentCode()));
        student.setFullName(normalize(request.getFullName()));
        student.setFirstName(normalizeNullable(request.getFirstName()));
        student.setMoeCode(normalizeNullable(request.getMoeCode()));
        student.setDateOfBirth(request.getDateOfBirth());
        student.setGender(request.getGender());
        student.setPlaceOfBirth(normalizeNullable(request.getPlaceOfBirth()));
        student.setEthnicity(normalizeNullable(request.getEthnicity()));
        student.setReligion(normalizeNullable(request.getReligion()));
        student.setNationality(normalizeNullable(request.getNationality()));
        student.setMobilePhone(normalizeNullable(request.getMobilePhone()));
        student.setEmail(normalizeNullable(request.getEmail()));
        // Avatar được xử lý riêng trong create() và update()
        student.setIdentityNumber(normalizeNullable(request.getIdentityNumber()));
        student.setIdentityIssueDate(request.getIdentityIssueDate());
        student.setIdentityIssuePlace(normalizeNullable(request.getIdentityIssuePlace()));
        student.setHealthInsuranceNumber(normalizeNullable(request.getHealthInsuranceNumber()));
        student.setBloodGroup(normalizeNullable(request.getBloodGroup()));
        student.setBoardingBook(normalizeNullable(request.getBoardingBook()));
        student.setAdmissionDate(request.getAdmissionDate());
        student.setStudentStatus(request.getStudentStatus());
        student.setAdmissionType(request.getAdmissionType());
        student.setUnit(unit);
    }

    private StudentEnrollment saveOrUpdateEnrollment(Student student, SchoolYear schoolYear, Classroom classroom,
            StudentEnrollmentCreateRequest request) {
        StudentEnrollment enrollment = studentEnrollmentRepository.findByStudentIdAndSchoolYearId(student.getId(),
                schoolYear.getId()).orElseGet(StudentEnrollment::new);
        enrollment.setStudent(student);
        enrollment.setSchoolYear(schoolYear);
        enrollment.setClassroom(classroom);
        enrollment.setEnrolledAt(request.getEnrolledAt());
        enrollment.setStatus(request.getStatus());
        enrollment.setIsRepeater(request.getIsRepeater());
        enrollment.setSessionsPerWeek(request.getSessionsPerWeek());
        enrollment.setStudyMode(request.getStudyMode());
        enrollment.setIsBoarding(request.getIsBoarding());
        enrollment.setIsTwoSessionsPerDay(request.getIsTwoSessionsPerDay());
        if (enrollment.getId() == null) {
            enrollment.setCreatedBy(getCurrentUsername());
        } else {
            enrollment.setUpdatedBy(getCurrentUsername());
        }
        return studentEnrollmentRepository.save(enrollment);
    }

    private List<StudentAddress> replaceAddresses(Student student, List<StudentAddressCreateRequest> requests) {
        studentAddressRepository.deleteByStudentId(student.getId());
        List<StudentAddress> addresses = safeList(requests).stream().map(request -> {
            StudentAddress address = new StudentAddress();
            address.setStudent(student);
            address.setAddressType(normalizeUpper(request.getAddressType()));
            address.setProvinceName(normalizeNullable(request.getProvinceName()));
            address.setWardName(normalizeNullable(request.getWardName()));
            address.setHamletName(normalizeNullable(request.getHamletName()));
            address.setDetailAddress(normalizeNullable(request.getDetailAddress()));
            return address;
        }).toList();
        return addresses.isEmpty() ? List.of() : studentAddressRepository.saveAll(addresses);
    }

    private List<StudentGuardian> replaceGuardians(Student student, List<StudentGuardianCreateRequest> requests) {
        studentGuardianRepository.deleteByStudentId(student.getId());
        List<StudentGuardian> guardians = safeList(requests).stream().map(request -> {
            StudentGuardian guardian = new StudentGuardian();
            guardian.setStudent(student);
            guardian.setGuardianType(normalizeUpper(request.getGuardianType()));
            guardian.setFullName(normalizeNullable(request.getFullName()));
            guardian.setBirthYear(request.getBirthYear());
            guardian.setOccupation(normalizeNullable(request.getOccupation()));
            guardian.setPhone(normalizeNullable(request.getPhone()));
            guardian.setEmail(normalizeNullable(request.getEmail()));
            guardian.setIdentityNumber(normalizeNullable(request.getIdentityNumber()));
            guardian.setIsEthnic(request.getIsEthnic() == null ? Boolean.FALSE : request.getIsEthnic());
            return guardian;
        }).toList();
        return guardians.isEmpty() ? List.of() : studentGuardianRepository.saveAll(guardians);
    }

    private StudentProfile replaceProfile(Student student, StudentProfileCreateRequest request) {
        if (request == null) {
            studentProfileRepository.findByStudentId(student.getId())
                    .ifPresent(studentProfileRepository::delete);
            return null;
        }

        StudentProfile profile = studentProfileRepository.findByStudentId(student.getId())
                .orElseGet(StudentProfile::new);
        profile.setStudent(student);
        profile.setPolicyObject(normalizeNullable(request.getPolicyObject()));
        profile.setPolicyBenefit(normalizeNullable(request.getPolicyBenefit()));
        profile.setPriorityCategory(normalizeNullable(request.getPriorityCategory()));
        profile.setStudentCategory(normalizeNullable(request.getStudentCategory()));
        profile.setRegionCategory(normalizeNullable(request.getRegionCategory()));
        profile.setDisabilityType(normalizeNullable(request.getDisabilityType()));
        profile.setDisabilityExemptEval(request.getDisabilityExemptEval());
        profile.setSupportTuitionCost(request.getSupportTuitionCost());
        profile.setResettlementArea(request.getResettlementArea());
        profile.setHousingSupport(request.getHousingSupport());
        profile.setMonthlyAllowance(request.getMonthlyAllowance());
        profile.setRiceSupport(request.getRiceSupport());
        profile.setFollowsMoeProgram(request.getFollowsMoeProgram());
        profile.setCanSwim(request.getCanSwim());
        profile.setLearnsEthnicLanguage(request.getLearnsEthnicLanguage());
        profile.setStudiedKindergarten5yo(request.getStudiedKindergarten5yo());
        profile.setNeedsVietnameseSupport(request.getNeedsVietnameseSupport());
        profile.setHasVietnameseReinforcementMaterial(request.getHasVietnameseReinforcementMaterial());
        profile.setHasEthnicTeachingAssistant(request.getHasEthnicTeachingAssistant());
        profile.setHasParentInternet(request.getHasParentInternet());
        profile.setHasParentSmartphone(request.getHasParentSmartphone());
        profile.setForeignLanguageProgram(normalizeNullable(request.getForeignLanguageProgram()));
        profile.setForeignLanguageCertificate(normalizeNullable(request.getForeignLanguageCertificate()));
        profile.setInformaticsCertificate(normalizeNullable(request.getInformaticsCertificate()));
        profile.setCareerOrientation(normalizeNullable(request.getCareerOrientation()));
        profile.setVocationalOrientation(normalizeNullable(request.getVocationalOrientation()));
        profile.setJoinedTeamDate(request.getJoinedTeamDate());
        profile.setOtherSystemCode(normalizeNullable(request.getOtherSystemCode()));
        profile.setSsoCode(normalizeNullable(request.getSsoCode()));
        return studentProfileRepository.save(profile);
    }

    private StudentItemDto toDto(Student student) {
        StudentEnrollment enrollment = findLatestEnrollment(student.getId());
        List<StudentAddress> addresses = studentAddressRepository.findByStudentIdOrderByIdAsc(student.getId());
        List<StudentGuardian> guardians = studentGuardianRepository.findByStudentIdOrderByIdAsc(student.getId());
        StudentProfile profile = studentProfileRepository.findByStudentId(student.getId()).orElse(null);
        return toDto(student, enrollment, addresses, guardians, profile);
    }

    private StudentItemDto toDto(Student student, StudentEnrollment enrollment, List<StudentAddress> addresses,
            List<StudentGuardian> guardians, StudentProfile profile) {
        return StudentItemDto.builder()
                .id(student.getId())
                .studentCode(student.getStudentCode())
                .fullName(student.getFullName())
                .firstName(student.getFirstName())
                .moeCode(student.getMoeCode())
                .dateOfBirth(student.getDateOfBirth())
                .gender(student.getGender())
                .placeOfBirth(student.getPlaceOfBirth())
                .ethnicity(student.getEthnicity())
                .religion(student.getReligion())
                .nationality(student.getNationality())
                .mobilePhone(student.getMobilePhone())
                .email(student.getEmail())
                .avatarUrl(student.getAvatarUrl())
                .identityNumber(student.getIdentityNumber())
                .identityIssueDate(student.getIdentityIssueDate())
                .identityIssuePlace(student.getIdentityIssuePlace())
                .healthInsuranceNumber(student.getHealthInsuranceNumber())
                .bloodGroup(student.getBloodGroup())
                .boardingBook(student.getBoardingBook())
                .admissionDate(student.getAdmissionDate())
                .studentStatus(student.getStudentStatus())
                .admissionType(student.getAdmissionType())
                .unitId(student.getUnit() == null ? null : student.getUnit().getId())
                .unitName(student.getUnit() == null ? null : student.getUnit().getName())
                .enrollment(toEnrollmentDto(enrollment))
                .addresses(addresses == null ? List.of() : addresses.stream().map(this::toAddressDto).toList())
                .guardians(guardians == null ? List.of() : guardians.stream().map(this::toGuardianDto).toList())
                .profile(toProfileDto(profile))
                .build();
    }

    private StudentEnrollmentItemDto toEnrollmentDto(StudentEnrollment enrollment) {
        if (enrollment == null) {
            return null;
        }
        return StudentEnrollmentItemDto.builder()
                .id(enrollment.getId())
                .schoolYearId(enrollment.getSchoolYear() == null ? null : enrollment.getSchoolYear().getId())
                .schoolYearName(enrollment.getSchoolYear() == null ? null : enrollment.getSchoolYear().getName())
                .classId(enrollment.getClassroom() == null ? null : enrollment.getClassroom().getId())
                .className(enrollment.getClassroom() == null ? null : enrollment.getClassroom().getName())
                .gradeLevelId(
                        enrollment.getClassroom() == null || enrollment.getClassroom().getGradeLevel() == null ? null
                                : enrollment.getClassroom().getGradeLevel().getId())
                .gradeLevelName(
                        enrollment.getClassroom() == null || enrollment.getClassroom().getGradeLevel() == null ? null
                                : enrollment.getClassroom().getGradeLevel().getName())
                .enrolledAt(enrollment.getEnrolledAt())
                .status(enrollment.getStatus())
                .isRepeater(enrollment.getIsRepeater())
                .sessionsPerWeek(enrollment.getSessionsPerWeek())
                .studyMode(enrollment.getStudyMode())
                .isBoarding(enrollment.getIsBoarding())
                .isTwoSessionsPerDay(enrollment.getIsTwoSessionsPerDay())
                .build();
    }

    private StudentAddressItemDto toAddressDto(StudentAddress address) {
        return StudentAddressItemDto.builder()
                .id(address.getId())
                .addressType(address.getAddressType())
                .provinceName(address.getProvinceName())
                .wardName(address.getWardName())
                .hamletName(address.getHamletName())
                .detailAddress(address.getDetailAddress())
                .build();
    }

    private StudentGuardianItemDto toGuardianDto(StudentGuardian guardian) {
        return StudentGuardianItemDto.builder()
                .id(guardian.getId())
                .guardianType(guardian.getGuardianType())
                .fullName(guardian.getFullName())
                .birthYear(guardian.getBirthYear())
                .occupation(guardian.getOccupation())
                .phone(guardian.getPhone())
                .email(guardian.getEmail())
                .identityNumber(guardian.getIdentityNumber())
                .isEthnic(guardian.getIsEthnic())
                .build();
    }

    private StudentProfileItemDto toProfileDto(StudentProfile profile) {
        if (profile == null) {
            return null;
        }
        return StudentProfileItemDto.builder()
                .id(profile.getId())
                .policyObject(profile.getPolicyObject())
                .policyBenefit(profile.getPolicyBenefit())
                .priorityCategory(profile.getPriorityCategory())
                .studentCategory(profile.getStudentCategory())
                .regionCategory(profile.getRegionCategory())
                .disabilityType(profile.getDisabilityType())
                .disabilityExemptEval(profile.getDisabilityExemptEval())
                .supportTuitionCost(profile.getSupportTuitionCost())
                .resettlementArea(profile.getResettlementArea())
                .housingSupport(profile.getHousingSupport())
                .monthlyAllowance(profile.getMonthlyAllowance())
                .riceSupport(profile.getRiceSupport())
                .followsMoeProgram(profile.getFollowsMoeProgram())
                .canSwim(profile.getCanSwim())
                .learnsEthnicLanguage(profile.getLearnsEthnicLanguage())
                .studiedKindergarten5yo(profile.getStudiedKindergarten5yo())
                .needsVietnameseSupport(profile.getNeedsVietnameseSupport())
                .hasVietnameseReinforcementMaterial(profile.getHasVietnameseReinforcementMaterial())
                .hasEthnicTeachingAssistant(profile.getHasEthnicTeachingAssistant())
                .hasParentInternet(profile.getHasParentInternet())
                .hasParentSmartphone(profile.getHasParentSmartphone())
                .foreignLanguageProgram(profile.getForeignLanguageProgram())
                .foreignLanguageCertificate(profile.getForeignLanguageCertificate())
                .informaticsCertificate(profile.getInformaticsCertificate())
                .careerOrientation(profile.getCareerOrientation())
                .vocationalOrientation(profile.getVocationalOrientation())
                .joinedTeamDate(profile.getJoinedTeamDate())
                .otherSystemCode(profile.getOtherSystemCode())
                .ssoCode(profile.getSsoCode())
                .build();
    }

    private int normalizePageSize(Integer pageSize) {
        return pageSize == null || pageSize <= 0 ? 10 : pageSize;
    }

    private int normalizePageNow(Integer pageNow) {
        return pageNow == null || pageNow <= 0 ? 1 : pageNow;
    }

    private <T> List<T> safeList(List<T> items) {
        return items == null ? List.of() : items;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String likeValue(String value) {
        return "%" + value.trim().toLowerCase(Locale.ROOT) + "%";
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }

    private String normalizeNullable(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    private String normalizeUpper(String value) {
        return normalize(value).toUpperCase(Locale.ROOT);
    }

    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null
                || "anonymousUser".equals(authentication.getName())) {
            return "SYSTEM";
        }
        return authentication.getName();
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] export(PageRequestDto<StudentFilterDto> request, Long unitId, ExportType exportType) {
        StudentFilterDto filter = request.getFilter() == null ? new StudentFilterDto() : request.getFilter();
        if (unitId != null) {
            filter.setUnitId(unitId);
        }
        PageRequestDto<StudentFilterDto> exportRequest = new PageRequestDto<>();
        exportRequest.setFilter(filter);
        exportRequest.setPageNow(1);
        exportRequest.setPageSize(Integer.MAX_VALUE);
        List<StudentItemDto> items = search(exportRequest).getItems();
        if (exportType == ExportType.PDF) {
            return buildStudentExportPdf(items, unitId);
        }
        return buildStudentExportWorkbook(items, unitId);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportReportCards(StudentReportCardExportRequest request, ExportType exportType) {
        List<Long> studentIds = normalizeStudentIds(request == null ? null : request.getStudentIds());
        List<StudentReportCardData> reportCards = new ArrayList<>();
        for (Long studentId : studentIds) {
            Student student = findStudent(studentId);
            validateStudentScope(ActionType.DOWNLOAD, student);
            reportCards.add(buildStudentReportCardData(student));
        }

        if (exportType == ExportType.PDF) {
            return buildStudentReportCardPdf(reportCards);
        }
        return buildStudentReportCardWorkbook(reportCards);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportExcelTemplate(Long unitId) {
        Unit unit = findUnit(unitId);
        try (Workbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("HocSinh");

            CellStyle govHeaderStyle = createExcelGovHeaderStyle(workbook);
            CellStyle govSubHeaderStyle = createExcelGovSubHeaderStyle(workbook);
            CellStyle titleStyle = createExcelTitleStyle(workbook);
            CellStyle headerStyle = createExcelHeaderStyle(workbook);
            CellStyle bodyStyle = createExcelBodyStyle(workbook);
            CellStyle guideStyle = createExcelGuideStyle(workbook);

            int middleColumn = STUDENT_IMPORT_LAST_DATA_COLUMN / 2;

            // ===== Dòng 0,1: Quốc hiệu =====
            Row govHeaderRow = sheet.createRow(0);
            createCell(govHeaderRow, 0, "BỘ GIÁO DỤC VÀ ĐÀO TẠO", govHeaderStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, middleColumn));

            createCell(govHeaderRow, middleColumn + 1, "CỘNG HÒA XÃ HỘI CHỦ NGHĨA VIỆT NAM", govHeaderStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, middleColumn + 1, STUDENT_IMPORT_LAST_DATA_COLUMN));

            Row govSubHeaderRow = sheet.createRow(1);
            createCell(govSubHeaderRow, 0, unit.getName(), govSubHeaderStyle);
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, middleColumn));

            createCell(govSubHeaderRow, middleColumn + 1, "Độc lập - Tự do - Hạnh phúc", govSubHeaderStyle);
            sheet.addMergedRegion(new CellRangeAddress(1, 1, middleColumn + 1, STUDENT_IMPORT_LAST_DATA_COLUMN));

            // ===== Tiêu đề =====
            createCell(sheet.createRow(3), 0, "MẪU IMPORT HỌC SINH", titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(3, 3, 0, STUDENT_IMPORT_LAST_DATA_COLUMN));

            createCell(sheet.createRow(4), 0,
                    "Cột bắt buộc: Họ tên, Ngày sinh, Năm học, Lớp. Mã học sinh để trống sẽ được tự sinh.",
                    guideStyle);
            sheet.addMergedRegion(new CellRangeAddress(4, 4, 0, STUDENT_IMPORT_LAST_DATA_COLUMN));

            createCell(sheet.createRow(5), 0,
                    "Giới tính: Nam/Nữ. Trạng thái học sinh: Đang học/Đã chuyển trường/Tạm nghỉ/Thôi học. Dân tộc cha/mẹ: nhập tên dân tộc, Kinh sẽ được hiểu là không phải DTTS.",
                    guideStyle);
            sheet.addMergedRegion(new CellRangeAddress(5, 5, 0, STUDENT_IMPORT_LAST_DATA_COLUMN));

            createCell(sheet.createRow(6), 0,
                    "Các cột dạng đánh dấu chỉ nhập X nếu có/đúng, để trống nếu không.",
                    guideStyle);
            sheet.addMergedRegion(new CellRangeAddress(6, 6, 0, STUDENT_IMPORT_LAST_DATA_COLUMN));
            // ===== Group header =====
            Row groupHeaderRow = sheet.createRow(STUDENT_TEMPLATE_GROUP_HEADER_ROW_INDEX);
            fillRowWithStyle(groupHeaderRow, 0, STUDENT_IMPORT_LAST_DATA_COLUMN, headerStyle);

            createCell(groupHeaderRow, 0, "THÔNG TIN HỌC SINH", headerStyle);
            sheet.addMergedRegion(new CellRangeAddress(
                    STUDENT_TEMPLATE_GROUP_HEADER_ROW_INDEX,
                    STUDENT_TEMPLATE_GROUP_HEADER_ROW_INDEX,
                    0, STUDENT_COL_ADDR_DETAIL));

            createCell(groupHeaderRow, STUDENT_COL_FATHER_NAME, "Cha", headerStyle);
            sheet.addMergedRegion(new CellRangeAddress(
                    STUDENT_TEMPLATE_GROUP_HEADER_ROW_INDEX,
                    STUDENT_TEMPLATE_GROUP_HEADER_ROW_INDEX,
                    STUDENT_COL_FATHER_NAME, STUDENT_COL_FATHER_IS_ETHNIC));

            createCell(groupHeaderRow, STUDENT_COL_MOTHER_NAME, "Mẹ", headerStyle);
            sheet.addMergedRegion(new CellRangeAddress(
                    STUDENT_TEMPLATE_GROUP_HEADER_ROW_INDEX,
                    STUDENT_TEMPLATE_GROUP_HEADER_ROW_INDEX,
                    STUDENT_COL_MOTHER_NAME, STUDENT_COL_MOTHER_IS_ETHNIC));

            createCell(groupHeaderRow, STUDENT_COL_PROFILE_POLICY_OBJECT, "Thông tin hỗ trợ", headerStyle);
            sheet.addMergedRegion(new CellRangeAddress(
                    STUDENT_TEMPLATE_GROUP_HEADER_ROW_INDEX,
                    STUDENT_TEMPLATE_GROUP_HEADER_ROW_INDEX,
                    STUDENT_COL_PROFILE_POLICY_OBJECT, STUDENT_COL_PROFILE_SSO_CODE));

            // ===== Header chi tiết =====
            Row headerRow = sheet.createRow(STUDENT_IMPORT_HEADER_ROW_INDEX);
            String[] headers = {
                    "STT",
                    "Họ tên *",
                    "Tên",
                    "Ngày sinh *",
                    "Giới tính",
                    "Điện thoại",
                    "Email",
                    "Năm học *",
                    "Lớp *",
                    "Ngày nhập học",
                    "Trạng thái học sinh",
                    "Mã MOET",
                    "Nơi sinh",
                    "Dân tộc",
                    "Tôn giáo",
                    "Quốc tịch",
                    "CCCD/CMND",
                    "Ngày cấp CCCD",
                    "Nơi cấp CCCD",
                    "Số BHYT",
                    "Nhóm máu",
                    "Sổ đăng bộ",
                    "Loại nhập học",
                    "Trạng thái nhập học",
                    "Lưu ban",
                    "Số buổi/tuần",
                    "Hình thức học",
                    "Bán trú",
                    "Học 2 buổi/ngày",
                    "Địa chỉ TT - Tỉnh/Thành",
                    "Địa chỉ TT - Xã/Phường",
                    "Địa chỉ TT - Thôn/Xóm",
                    "Địa chỉ TT - Chi tiết",
                    "Cha - Họ tên",
                    "Cha - Năm sinh",
                    "Cha - Nghề nghiệp",
                    "Cha - Điện thoại",
                    "Cha - Email",
                    "Cha - CCCD",
                    "Cha - Dân tộc",
                    "Mẹ - Họ tên",
                    "Mẹ - Năm sinh",
                    "Mẹ - Nghề nghiệp",
                    "Mẹ - Điện thoại",
                    "Mẹ - Email",
                    "Mẹ - CCCD",
                    "Mẹ - Dân tộc",
                    "Đối tượng chính sách",
                    "Chế độ chính sách",
                    "Diện ưu tiên",
                    "Khu vực",
                    "Loại khuyết tật",
                    "Miễn đánh giá KT",
                    "Hỗ trợ chi phí",
                    "Có Internet tại nhà",
                    "Có smartphone cha/mẹ",
                    "Mã hệ thống khác",
                    "Mã SSO"
            };

            for (int i = 0; i < headers.length; i++) {
                createCell(headerRow, i, headers[i], headerStyle);
            }

            // ===== Dòng mẫu =====
            Row sampleRow = sheet.createRow(STUDENT_IMPORT_DATA_START_ROW_INDEX);
            Object[] sampleValues = {
                    1,
                    "Nguyễn Văn A",
                    "Văn A",
                    "01/09/2015",
                    "Nam",
                    "0912345678",
                    "a@example.com",
                    "2025 - 2026",
                    "1A1",
                    "05/09/2021",
                    "Đang học",
                    "MOET001",
                    "Đắk Lắk",
                    "Kinh",
                    "Không",
                    "Việt Nam",
                    "079205001234",
                    "10/10/2022",
                    "Cục CSQLHC",
                    "BHYT0001",
                    "O",
                    "SD001",
                    "1",
                    "1",
                    booleanMark(false), // Lưu ban
                    "9",
                    "1",
                    booleanMark(true), // Bán trú
                    booleanMark(true), // Học 2 buổi/ngày
                    "Đắk Lắk",
                    "Phường 1",
                    "Thôn 3",
                    "Số 10 Đường A",
                    "Nguyễn Văn B",
                    1980,
                    "Nông dân",
                    "0900000001",
                    "b@example.com",
                    "079205009999",
                    "Kinh", // Cha - Dân tộc
                    "Trần Thị C",
                    1982,
                    "Nội trợ",
                    "0900000002",
                    "c@example.com",
                    "079205008888",
                    "Kinh", // Mẹ - Dân tộc
                    "Hộ nghèo",
                    "Miễn học phí",
                    "Ưu tiên 1",
                    "KV1",
                    "Không",
                    booleanMark(false), // Miễn đánh giá KT
                    booleanMark(true), // Hỗ trợ chi phí
                    booleanMark(true), // Có Internet tại nhà
                    booleanMark(true), // Có smartphone cha/mẹ
                    "HS-EXT-001",
                    "SSO-001"
            };

            for (int i = 0; i < sampleValues.length; i++) {
                createCell(sampleRow, i, sampleValues[i], bodyStyle);
            }

            sheet.createFreezePane(0, STUDENT_IMPORT_DATA_START_ROW_INDEX);
            autosize(sheet, headers.length);

            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException ex) {
            throw new UserMessageException("Không thể tạo file Excel mẫu import học sinh");
        }
    }

    @Override
    @Transactional
    public StudentImportResultDto importExcel(Long unitId, MultipartFile file) {
        findUnit(unitId);
        validateExcelFile(file);
        int successCount = 0;
        Map<Integer, String> rowErrors = new java.util.LinkedHashMap<>();

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();
            for (int rowIndex = STUDENT_IMPORT_DATA_START_ROW_INDEX; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (isExcelRowEmpty(row, STUDENT_COL_FULL_NAME, STUDENT_IMPORT_LAST_DATA_COLUMN, formatter)) {
                    continue;
                }
                try {
                    upsertStudentFromExcelRow(unitId, row, formatter);
                    successCount++;
                } catch (Exception ex) {
                    rowErrors.put(rowIndex, ex.getMessage());
                }
            }

            String token = null;
            String fileName = null;
            if (!rowErrors.isEmpty()) {
                byte[] errorFile = buildStudentImportErrorFile(workbook, sheet, rowErrors);
                fileName = "student-import-errors.xlsx";
                token = importErrorFileStorageService.store(fileName, EXCEL_CONTENT_TYPE, errorFile);
            }

            return StudentImportResultDto.builder()
                    .successCount(successCount)
                    .failedCount(rowErrors.size())
                    .hasErrorFile(token != null)
                    .errorFileToken(token)
                    .errorFileName(fileName)
                    .build();
        } catch (IOException ex) {
            throw new UserMessageException("Khong doc duoc file Excel hoc sinh");
        }
    }

    @Override
    public TemporaryFileDto getImportErrorFile(String token) {
        return importErrorFileStorageService.get(token);
    }

    private byte[] buildStudentExportWorkbook(List<StudentItemDto> items, Long unitId) {
        Unit unit = unitId == null ? null : findUnit(unitId);

        try (Workbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("HocSinh");

            CellStyle infoStyle = createExcelInfoStyle(workbook);
            CellStyle govHeaderStyle = createExcelGovHeaderStyle(workbook);
            CellStyle govSubHeaderStyle = createExcelGovSubHeaderStyle(workbook);
            CellStyle titleStyle = createExcelTitleStyle(workbook);
            CellStyle headerStyle = createExcelHeaderStyle(workbook);
            CellStyle bodyStyle = createExcelBodyStyle(workbook);

            int lastColumn = STUDENT_IMPORT_LAST_DATA_COLUMN;
            int middleColumn = lastColumn / 2;

            Row infoRow = sheet.createRow(0);
            createCell(infoRow, 0, buildExportInfoLine(), infoStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, lastColumn));

            Row govHeaderRow = sheet.createRow(1);
            createCell(govHeaderRow, 0, "BỘ GIÁO DỤC VÀ ĐÀO TẠO", govHeaderStyle);
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, middleColumn));
            createCell(govHeaderRow, middleColumn + 1, "CỘNG HÒA XÃ HỘI CHỦ NGHĨA VIỆT NAM", govHeaderStyle);
            sheet.addMergedRegion(new CellRangeAddress(1, 1, middleColumn + 1, lastColumn));

            Row govSubHeaderRow = sheet.createRow(2);
            createCell(govSubHeaderRow, 0, unit == null ? "" : unit.getName(), govSubHeaderStyle);
            sheet.addMergedRegion(new CellRangeAddress(2, 2, 0, middleColumn));
            createCell(govSubHeaderRow, middleColumn + 1, "Độc lập - Tự do - Hạnh phúc", govSubHeaderStyle);
            sheet.addMergedRegion(new CellRangeAddress(2, 2, middleColumn + 1, lastColumn));

            createCell(sheet.createRow(4), 0, "DANH SÁCH HỌC SINH", titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(4, 4, 0, lastColumn));

            Row groupHeaderRow = sheet.createRow(6);
            fillRowWithStyle(groupHeaderRow, 0, lastColumn, headerStyle);
            createCell(groupHeaderRow, 0, "THÔNG TIN HỌC SINH", headerStyle);
            sheet.addMergedRegion(new CellRangeAddress(6, 6, 0, STUDENT_COL_ADDR_DETAIL));
            createCell(groupHeaderRow, STUDENT_COL_FATHER_NAME, "Cha", headerStyle);
            sheet.addMergedRegion(new CellRangeAddress(6, 6, STUDENT_COL_FATHER_NAME, STUDENT_COL_FATHER_IS_ETHNIC));
            createCell(groupHeaderRow, STUDENT_COL_MOTHER_NAME, "Mẹ", headerStyle);
            sheet.addMergedRegion(new CellRangeAddress(6, 6, STUDENT_COL_MOTHER_NAME, STUDENT_COL_MOTHER_IS_ETHNIC));
            createCell(groupHeaderRow, STUDENT_COL_PROFILE_POLICY_OBJECT, "Thông tin hỗ trợ", headerStyle);
            sheet.addMergedRegion(
                    new CellRangeAddress(6, 6, STUDENT_COL_PROFILE_POLICY_OBJECT, STUDENT_COL_PROFILE_SSO_CODE));

            Row headerRow = sheet.createRow(7);
            String[] headers = buildStudentExportHeaders();
            for (int i = 0; i < headers.length; i++) {
                createCell(headerRow, i, headers[i], headerStyle);
            }

            int rowIndex = 8;
            int stt = 1;
            for (StudentItemDto item : items) {
                Row row = sheet.createRow(rowIndex++);
                Object[] values = buildStudentExportRow(item, stt++);
                for (int i = 0; i < values.length; i++) {
                    createCell(row, i, values[i], bodyStyle);
                }
            }

            autosize(sheet, headers.length);
            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException ex) {
            throw new UserMessageException("Không thể export học sinh");
        }
    }

    private String[] buildStudentExportHeaders() {
        return new String[] {
                "STT", "Họ tên", "Tên", "Ngày sinh", "Giới tính", "Điện thoại", "Email", "Năm học", "Lớp",
                "Ngày nhập học", "Trạng thái học sinh", "Mã MOET", "Nơi sinh", "Dân tộc", "Tôn giáo", "Quốc tịch",
                "CCCD/CMND", "Ngày cấp CCCD", "Nơi cấp CCCD", "Số BHYT", "Nhóm máu", "Sổ đăng bộ", "Loại nhập học",
                "Trạng thái nhập học", "Lưu ban", "Số buổi/tuần", "Hình thức học", "Bán trú", "Học 2 buổi/ngày",
                "Địa chỉ TT - Tỉnh/Thành", "Địa chỉ TT - Xã/Phường", "Địa chỉ TT - Thôn/Xóm", "Địa chỉ TT - Chi tiết",
                "Cha - Họ tên", "Cha - Năm sinh", "Cha - Nghề nghiệp", "Cha - Điện thoại", "Cha - Email", "Cha - CCCD",
                "Cha - Dân tộc", "Mẹ - Họ tên", "Mẹ - Năm sinh", "Mẹ - Nghề nghiệp", "Mẹ - Điện thoại", "Mẹ - Email",
                "Mẹ - CCCD", "Mẹ - Dân tộc", "Đối tượng chính sách", "Chế độ chính sách", "Diện ưu tiên", "Khu vực",
                "Loại khuyết tật", "Miễn đánh giá KT", "Hỗ trợ chi phí", "Có Internet tại nhà", "Có smartphone cha/mẹ",
                "Mã hệ thống khác", "Mã SSO"
        };
    }

    private Object[] buildStudentExportRow(StudentItemDto item, int stt) {
        StudentEnrollmentItemDto enrollment = item.getEnrollment();
        StudentAddressItemDto permanentAddress = findStudentAddress(item.getAddresses(), ADDRESS_TYPE_PERMANENT);
        StudentGuardianItemDto father = findStudentGuardian(item.getGuardians(), GUARDIAN_TYPE_FATHER);
        StudentGuardianItemDto mother = findStudentGuardian(item.getGuardians(), GUARDIAN_TYPE_MOTHER);
        StudentProfileItemDto profile = item.getProfile();

        Object[] values = new Object[STUDENT_IMPORT_LAST_DATA_COLUMN + 1];
        values[0] = stt;
        values[1] = item.getFullName();
        values[2] = item.getFirstName();
        values[3] = formatDate(item.getDateOfBirth());
        values[4] = studentGenderLabel(item.getGender());
        values[5] = item.getMobilePhone();
        values[6] = item.getEmail();
        values[7] = enrollment == null ? null : enrollment.getSchoolYearName();
        values[8] = enrollment == null ? null : enrollment.getClassName();
        values[9] = enrollment == null ? null : formatDate(enrollment.getEnrolledAt());
        values[10] = studentStatusLabel(item.getStudentStatus());
        values[11] = item.getMoeCode();
        values[12] = item.getPlaceOfBirth();
        values[13] = item.getEthnicity();
        values[14] = item.getReligion();
        values[15] = item.getNationality();
        values[16] = item.getIdentityNumber();
        values[17] = formatDate(item.getIdentityIssueDate());
        values[18] = item.getIdentityIssuePlace();
        values[19] = item.getHealthInsuranceNumber();
        values[20] = item.getBloodGroup();
        values[21] = item.getBoardingBook();
        values[22] = item.getAdmissionType();
        values[23] = enrollment == null ? null : enrollment.getStatus();
        values[24] = booleanMark(enrollment != null && Boolean.TRUE.equals(enrollment.getIsRepeater()));
        values[25] = enrollment == null ? null : enrollment.getSessionsPerWeek();
        values[26] = enrollment == null ? null : enrollment.getStudyMode();
        values[27] = booleanMark(enrollment != null && Boolean.TRUE.equals(enrollment.getIsBoarding()));
        values[28] = booleanMark(enrollment != null && Boolean.TRUE.equals(enrollment.getIsTwoSessionsPerDay()));
        values[29] = permanentAddress == null ? null : permanentAddress.getProvinceName();
        values[30] = permanentAddress == null ? null : permanentAddress.getWardName();
        values[31] = permanentAddress == null ? null : permanentAddress.getHamletName();
        values[32] = permanentAddress == null ? null : permanentAddress.getDetailAddress();
        values[33] = father == null ? null : father.getFullName();
        values[34] = father == null ? null : father.getBirthYear();
        values[35] = father == null ? null : father.getOccupation();
        values[36] = father == null ? null : father.getPhone();
        values[37] = father == null ? null : father.getEmail();
        values[38] = father == null ? null : father.getIdentityNumber();
        values[39] = father == null ? null : guardianEthnicLabel(father.getIsEthnic());
        values[40] = mother == null ? null : mother.getFullName();
        values[41] = mother == null ? null : mother.getBirthYear();
        values[42] = mother == null ? null : mother.getOccupation();
        values[43] = mother == null ? null : mother.getPhone();
        values[44] = mother == null ? null : mother.getEmail();
        values[45] = mother == null ? null : mother.getIdentityNumber();
        values[46] = mother == null ? null : guardianEthnicLabel(mother.getIsEthnic());
        values[47] = profile == null ? null : profile.getPolicyObject();
        values[48] = profile == null ? null : profile.getPolicyBenefit();
        values[49] = profile == null ? null : profile.getPriorityCategory();
        values[50] = profile == null ? null : profile.getRegionCategory();
        values[51] = profile == null ? null : profile.getDisabilityType();
        values[52] = booleanMark(profile != null && Boolean.TRUE.equals(profile.getDisabilityExemptEval()));
        values[53] = booleanMark(profile != null && Boolean.TRUE.equals(profile.getSupportTuitionCost()));
        values[54] = booleanMark(profile != null && Boolean.TRUE.equals(profile.getHasParentInternet()));
        values[55] = booleanMark(profile != null && Boolean.TRUE.equals(profile.getHasParentSmartphone()));
        values[56] = profile == null ? null : profile.getOtherSystemCode();
        values[57] = profile == null ? null : profile.getSsoCode();
        return values;
    }

    private StudentAddressItemDto findStudentAddress(List<StudentAddressItemDto> addresses, String addressType) {
        if (addresses == null) {
            return null;
        }
        return addresses.stream()
                .filter(item -> addressType.equals(item.getAddressType()))
                .findFirst()
                .orElse(null);
    }

    private StudentGuardianItemDto findStudentGuardian(List<StudentGuardianItemDto> guardians, String guardianType) {
        if (guardians == null) {
            return null;
        }
        return guardians.stream()
                .filter(item -> guardianType.equals(item.getGuardianType()))
                .findFirst()
                .orElse(null);
    }

    private String guardianEthnicLabel(Boolean isEthnic) {
        if (isEthnic == null) {
            return null;
        }
        return Boolean.TRUE.equals(isEthnic) ? "Khác Kinh" : "Kinh";
    }

    private byte[] buildStudentExportPdf(List<StudentItemDto> items, Long unitId) {
        Unit unit = unitId == null ? null : findUnit(unitId);
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4.rotate(), 24, 24, 20, 20);
            PdfWriter.getInstance(document, outputStream);
            document.open();

            com.lowagie.text.Font titleFont = createPdfFont(16, com.lowagie.text.Font.BOLD);
            com.lowagie.text.Font headerFont = createPdfFont(9, com.lowagie.text.Font.BOLD);
            com.lowagie.text.Font bodyFont = createPdfFont(9, com.lowagie.text.Font.NORMAL);
            com.lowagie.text.Font infoFont = createPdfFont(10, com.lowagie.text.Font.ITALIC);

            Paragraph gov1 = new Paragraph("BỘ GIÁO DỤC VÀ ĐÀO TẠO", headerFont);
            gov1.setAlignment(Element.ALIGN_CENTER);
            document.add(gov1);

            Paragraph gov2 = new Paragraph(unit == null ? "" : unit.getName(), bodyFont);
            gov2.setAlignment(Element.ALIGN_CENTER);
            gov2.setSpacingAfter(6f);
            document.add(gov2);

            Paragraph info = new Paragraph(buildExportInfoLine(), infoFont);
            info.setAlignment(Element.ALIGN_RIGHT);
            info.setSpacingAfter(6f);
            document.add(info);

            Paragraph title = new Paragraph("DANH SÁCH HỌC SINH", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(12f);
            document.add(title);

            PdfPTable table = new PdfPTable(
                    new float[] { 0.7f, 1.3f, 2.5f, 1.3f, 1.1f, 1.5f, 2.0f, 2.0f, 1.4f, 1.4f, 1.6f, 1.4f });
            table.setWidthPercentage(100);
            String[] headers = { "STT", "Mã HS", "Họ tên", "Ngày sinh", "Giới tính", "Điện thoại", "Email", "Đơn vị",
                    "Năm học", "Lớp", "Ngày nhập học", "Trạng thái" };
            for (String header : headers) {
                addPdfHeaderCell(table, header, headerFont);
            }

            int stt = 1;
            for (StudentItemDto item : items) {
                addPdfBodyCell(table, String.valueOf(stt++), bodyFont, Element.ALIGN_CENTER);
                addPdfBodyCell(table, item.getStudentCode(), bodyFont, Element.ALIGN_LEFT);
                addPdfBodyCell(table, item.getFullName(), bodyFont, Element.ALIGN_LEFT);
                addPdfBodyCell(table, formatDate(item.getDateOfBirth()), bodyFont, Element.ALIGN_CENTER);
                addPdfBodyCell(table, studentGenderLabel(item.getGender()), bodyFont, Element.ALIGN_CENTER);
                addPdfBodyCell(table, item.getMobilePhone(), bodyFont, Element.ALIGN_LEFT);
                addPdfBodyCell(table, item.getEmail(), bodyFont, Element.ALIGN_LEFT);
                addPdfBodyCell(table, item.getUnitName(), bodyFont, Element.ALIGN_LEFT);
                addPdfBodyCell(table, item.getEnrollment() == null ? null : item.getEnrollment().getSchoolYearName(),
                        bodyFont, Element.ALIGN_LEFT);
                addPdfBodyCell(table, item.getEnrollment() == null ? null : item.getEnrollment().getClassName(),
                        bodyFont, Element.ALIGN_LEFT);
                addPdfBodyCell(table,
                        item.getEnrollment() == null ? null : formatDate(item.getEnrollment().getEnrolledAt()),
                        bodyFont, Element.ALIGN_CENTER);
                addPdfBodyCell(table, studentStatusLabel(item.getStudentStatus()), bodyFont, Element.ALIGN_CENTER);
            }

            document.add(table);
            document.close();
            return outputStream.toByteArray();
        } catch (DocumentException | IOException ex) {
            throw new UserMessageException("Không thể export PDF học sinh");
        }
    }

    private StudentReportCardData buildStudentReportCardData(Student student) {
        StudentEnrollment latestEnrollment = findLatestEnrollment(student.getId());
        if (latestEnrollment == null) {
            throw new UserMessageException("Hoc sinh " + student.getFullName() + " chua co thong tin nhap hoc");
        }

        List<StudentEnrollment> histories = new ArrayList<>(
                studentEnrollmentRepository.findByStudentIdOrderBySchoolYearIdDescIdDesc(student.getId()));
        histories.sort(Comparator
                .comparing((StudentEnrollment item) -> item.getSchoolYear() == null ? null : item.getSchoolYear().getStartDate(),
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(StudentEnrollment::getId, Comparator.nullsLast(Comparator.naturalOrder())));

        List<StudentAddress> addresses = studentAddressRepository.findByStudentIdOrderByIdAsc(student.getId());
        List<StudentGuardian> guardians = studentGuardianRepository.findByStudentIdOrderByIdAsc(student.getId());
        StudentProfile profile = studentProfileRepository.findByStudentId(student.getId()).orElse(null);
        List<ReportCardClassPage> classPages = new ArrayList<>();
        for (StudentEnrollment history : histories) {
            classPages.add(buildReportCardClassPage(student, history, latestEnrollment));
        }

        Long classroomId = latestEnrollment.getClassroom() == null ? null : latestEnrollment.getClassroom().getId();
        List<ClassroomSubject> classroomSubjects = classroomId == null
                ? List.of()
                : classroomSubjectRepository.findByClassroomIdAndStatusAndDeletedFlagOrderBySubjectNameAsc(classroomId, 1, 0);
        List<StudentEvaluation> evaluations = classroomId == null
                ? List.of()
                : studentEvaluationRepository
                        .findByStudentIdAndClassroomIdAndDeletedFlagOrderBySemesterSemesterOrderAscSubjectNameAsc(
                                student.getId(), classroomId, 0);
        List<AttendanceRecord> attendanceRecords = classroomId == null
                ? List.of()
                : attendanceRecordRepository.findByClassroomIdAndStudentIdAndDeletedFlag(classroomId, student.getId(), 0);

        return new StudentReportCardData(
                student,
                latestEnrollment,
                histories,
                findPermanentAddress(addresses),
                findGuardianByType(guardians, GUARDIAN_TYPE_FATHER),
                findGuardianByType(guardians, GUARDIAN_TYPE_MOTHER),
                findGuardianByType(guardians, GUARDIAN_TYPE_GUARDIAN),
                profile,
                buildReportCardEvaluationRows(classroomSubjects, evaluations),
                buildAttendanceSummary(attendanceRecords, latestEnrollment.getSchoolYear()),
                buildReportCardConclusion(student, latestEnrollment),
                classPages);
    }

    private ReportCardClassPage buildReportCardClassPage(Student student, StudentEnrollment enrollment,
            StudentEnrollment latestEnrollment) {
        Long classroomId = enrollment.getClassroom() == null ? null : enrollment.getClassroom().getId();
        List<ClassroomSubject> classroomSubjects = classroomId == null
                ? List.of()
                : classroomSubjectRepository.findByClassroomIdAndStatusAndDeletedFlagOrderBySubjectNameAsc(classroomId, 1, 0);
        List<StudentEvaluation> evaluations = classroomId == null
                ? List.of()
                : studentEvaluationRepository
                        .findByStudentIdAndClassroomIdAndDeletedFlagOrderBySemesterSemesterOrderAscSubjectNameAsc(
                                student.getId(), classroomId, 0);
        List<AttendanceRecord> attendanceRecords = classroomId == null
                ? List.of()
                : attendanceRecordRepository.findByClassroomIdAndStudentIdAndDeletedFlag(classroomId, student.getId(), 0);
        boolean latestClassPage = latestEnrollment != null && latestEnrollment.getId() != null
                && latestEnrollment.getId().equals(enrollment.getId());
        String conclusion = latestClassPage
                ? buildReportCardConclusion(student, enrollment)
                : (Boolean.TRUE.equals(enrollment.getIsRepeater()) ? "Luu ban" : "");

        return new ReportCardClassPage(
                enrollment,
                buildReportCardEvaluationRows(classroomSubjects, evaluations),
                buildAttendanceSummary(attendanceRecords, enrollment.getSchoolYear()),
                conclusion);
    }

    private List<ReportCardEvaluationRow> buildReportCardEvaluationRows(List<ClassroomSubject> classroomSubjects,
            List<StudentEvaluation> evaluations) {
        Map<Long, Map<Integer, StudentEvaluation>> evaluationsBySubjectAndSemester = new LinkedHashMap<>();
        Map<Long, String> subjectNames = new LinkedHashMap<>();

        for (ClassroomSubject classroomSubject : safeList(classroomSubjects)) {
            if (classroomSubject == null || classroomSubject.getSubject() == null
                    || classroomSubject.getSubject().getId() == null) {
                continue;
            }
            subjectNames.put(classroomSubject.getSubject().getId(), classroomSubject.getSubject().getName());
        }

        for (StudentEvaluation evaluation : safeList(evaluations)) {
            if (evaluation == null || evaluation.getSubject() == null || evaluation.getSubject().getId() == null) {
                continue;
            }
            Long subjectId = evaluation.getSubject().getId();
            Integer semesterOrder = evaluation.getSemester() == null ? null : evaluation.getSemester().getSemesterOrder();
            subjectNames.putIfAbsent(subjectId, evaluation.getSubject().getName());
            if (semesterOrder == null) {
                continue;
            }
            evaluationsBySubjectAndSemester
                    .computeIfAbsent(subjectId, ignored -> new LinkedHashMap<>())
                    .put(semesterOrder, evaluation);
        }

        List<ReportCardEvaluationRow> rows = new ArrayList<>();
        for (Map.Entry<Long, String> subjectEntry : subjectNames.entrySet()) {
            Map<Integer, StudentEvaluation> subjectEvaluations = evaluationsBySubjectAndSemester
                    .getOrDefault(subjectEntry.getKey(), Map.of());
            StudentEvaluation semesterOne = subjectEvaluations.get(1);
            StudentEvaluation semesterTwo = subjectEvaluations.get(2);
            rows.add(new ReportCardEvaluationRow(
                    subjectEntry.getValue(),
                    formatEvaluationValue(semesterOne, true),
                    formatEvaluationValue(semesterOne, false),
                    formatEvaluationValue(semesterTwo, true),
                    formatEvaluationValue(semesterTwo, false),
                    buildEvaluationRemark(semesterOne, semesterTwo)));
        }
        return rows;
    }

    private AttendanceSummary buildAttendanceSummary(List<AttendanceRecord> attendanceRecords, SchoolYear schoolYear) {
        int excused = 0;
        int unexcused = 0;
        LocalDate startDate = schoolYear == null ? null : schoolYear.getStartDate();
        LocalDate endDate = schoolYear == null ? null : schoolYear.getEndDate();

        for (AttendanceRecord attendanceRecord : safeList(attendanceRecords)) {
            if (attendanceRecord == null || attendanceRecord.getAttendanceDate() == null) {
                continue;
            }
            if (startDate != null && attendanceRecord.getAttendanceDate().isBefore(startDate)) {
                continue;
            }
            if (endDate != null && attendanceRecord.getAttendanceDate().isAfter(endDate)) {
                continue;
            }

            String status = normalizeUpper(attendanceRecord.getAttendanceStatus());
            if ("P".equals(status)) {
                excused++;
            } else if ("K".equals(status)) {
                unexcused++;
            }
        }
        return new AttendanceSummary(excused, unexcused);
    }

    private String buildReportCardConclusion(Student student, StudentEnrollment enrollment) {
        if (Boolean.TRUE.equals(enrollment.getIsRepeater())) {
            return "Luu ban";
        }
        if (student.getStudentStatus() == null) {
            return "Dang hoc";
        }
        return switch (student.getStudentStatus()) {
            case 0 -> "Hoan thanh chuong trinh lop hoc";
            case 1 -> "Da chuyen truong";
            case 2 -> "Tam nghi";
            case 3 -> "Thoi hoc";
            default -> "Trang thai: " + student.getStudentStatus();
        };
    }

    private byte[] buildStudentReportCardWorkbook(List<StudentReportCardData> reportCards) {
        try (Workbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            CellStyle infoStyle = createExcelInfoStyle(workbook);
            CellStyle govHeaderStyle = createExcelGovHeaderStyle(workbook);
            CellStyle govSubHeaderStyle = createExcelGovSubHeaderStyle(workbook);
            CellStyle titleStyle = createExcelTitleStyle(workbook);
            CellStyle sectionStyle = createExcelSectionStyle(workbook);
            CellStyle headerStyle = createExcelHeaderStyle(workbook);
            CellStyle labelStyle = createExcelLabelCellStyle(workbook);
            CellStyle bodyStyle = createExcelBodyStyle(workbook);
            CellStyle centerBodyStyle = createExcelCenteredBodyStyle(workbook);

            int sheetIndex = 1;
            for (StudentReportCardData reportCard : reportCards) {
                Sheet sheet = workbook.createSheet(buildReportCardSheetName(reportCard.getStudent().getFullName(), sheetIndex++));
                sheet.setDisplayGridlines(false);
                for (int column = 0; column <= 5; column++) {
                    sheet.setColumnWidth(column, switch (column) {
                        case 0, 2, 4 -> 5200;
                        case 1, 3, 5 -> 7800;
                        default -> 6000;
                    });
                }

                int rowIndex = 0;
                Row infoRow = sheet.createRow(rowIndex++);
                createCell(infoRow, 0, buildExportInfoLine(), infoStyle);
                sheet.addMergedRegion(new CellRangeAddress(infoRow.getRowNum(), infoRow.getRowNum(), 0, 5));

                Row govHeaderRow = sheet.createRow(rowIndex++);
                createCell(govHeaderRow, 0, "BỘ GIÁO DỤC VÀ ĐÀO TẠO", govHeaderStyle);
                createCell(govHeaderRow, 3, "CỘNG HÒA XÃ HỘI CHỦ NGHĨA VIỆT NAM", govHeaderStyle);
                sheet.addMergedRegion(new CellRangeAddress(govHeaderRow.getRowNum(), govHeaderRow.getRowNum(), 0, 2));
                sheet.addMergedRegion(new CellRangeAddress(govHeaderRow.getRowNum(), govHeaderRow.getRowNum(), 3, 5));

                Row govSubHeaderRow = sheet.createRow(rowIndex++);
                createCell(govSubHeaderRow, 0, reportCard.getStudent().getUnit() == null ? "" : reportCard.getStudent().getUnit().getName(),
                        govSubHeaderStyle);
                createCell(govSubHeaderRow, 3, "Độc lập - Tự do - Hạnh phúc", govSubHeaderStyle);
                sheet.addMergedRegion(new CellRangeAddress(govSubHeaderRow.getRowNum(), govSubHeaderRow.getRowNum(), 0, 2));
                sheet.addMergedRegion(new CellRangeAddress(govSubHeaderRow.getRowNum(), govSubHeaderRow.getRowNum(), 3, 5));

                rowIndex++;
                Row titleRow = sheet.createRow(rowIndex++);
                createCell(titleRow, 0, "HỌC BẠ TIỂU HỌC", titleStyle);
                sheet.addMergedRegion(new CellRangeAddress(titleRow.getRowNum(), titleRow.getRowNum(), 0, 5));

                Row nameRow = sheet.createRow(rowIndex++);
                createCell(nameRow, 0, reportCard.getStudent().getFullName(), titleStyle);
                sheet.addMergedRegion(new CellRangeAddress(nameRow.getRowNum(), nameRow.getRowNum(), 0, 5));

                rowIndex++;
                Row sectionInfoRow = sheet.createRow(rowIndex++);
                createCell(sectionInfoRow, 0, "THÔNG TIN HỌC SINH", sectionStyle);
                sheet.addMergedRegion(new CellRangeAddress(sectionInfoRow.getRowNum(), sectionInfoRow.getRowNum(), 0, 5));

                rowIndex = writeInfoRow(sheet, rowIndex, labelStyle, bodyStyle,
                        "Họ và tên", reportCard.getStudent().getFullName(),
                        "Mã học sinh", reportCard.getStudent().getStudentCode(),
                        "Ngày sinh", formatDate(reportCard.getStudent().getDateOfBirth()));
                rowIndex = writeInfoRow(sheet, rowIndex, labelStyle, bodyStyle,
                        "Giới tính", studentGenderLabel(reportCard.getStudent().getGender()),
                        "Dân tộc", reportCard.getStudent().getEthnicity(),
                        "Quốc tịch", reportCard.getStudent().getNationality());
                rowIndex = writeInfoRow(sheet, rowIndex, labelStyle, bodyStyle,
                        "Nơi sinh", reportCard.getStudent().getPlaceOfBirth(),
                        "Địa chỉ", buildFullAddress(reportCard.getPermanentAddress()),
                        "Mã MOET", reportCard.getStudent().getMoeCode());
                rowIndex = writeInfoRow(sheet, rowIndex, labelStyle, bodyStyle,
                        "Cha", guardianSummary(reportCard.getFather()),
                        "Mẹ", guardianSummary(reportCard.getMother()),
                        "Đơn vị", reportCard.getStudent().getUnit() == null ? null : reportCard.getStudent().getUnit().getName());

                rowIndex++;
                Row historySectionRow = sheet.createRow(rowIndex++);
                createCell(historySectionRow, 0, "QUÁ TRÌNH HỌC TẬP", sectionStyle);
                sheet.addMergedRegion(new CellRangeAddress(historySectionRow.getRowNum(), historySectionRow.getRowNum(), 0, 5));

                Row historyHeaderRow = sheet.createRow(rowIndex++);
                String[] historyHeaders = { "Năm học", "Lớp", "Trường", "Sổ đăng bộ", "Ngày vào học/chuyển đến", "Ghi chú" };
                for (int i = 0; i < historyHeaders.length; i++) {
                    createCell(historyHeaderRow, i, historyHeaders[i], headerStyle);
                }
                if (reportCard.getHistories().isEmpty()) {
                    Row row = sheet.createRow(rowIndex++);
                    for (int i = 0; i < historyHeaders.length; i++) {
                        createCell(row, i, "", bodyStyle);
                    }
                } else {
                    for (StudentEnrollment history : reportCard.getHistories()) {
                        Row row = sheet.createRow(rowIndex++);
                        createCell(row, 0, history.getSchoolYear() == null ? null : history.getSchoolYear().getName(), bodyStyle);
                        createCell(row, 1, history.getClassroom() == null ? null : history.getClassroom().getName(), bodyStyle);
                        createCell(row, 2, reportCard.getStudent().getUnit() == null ? null : reportCard.getStudent().getUnit().getName(),
                                bodyStyle);
                        createCell(row, 3, reportCard.getStudent().getBoardingBook(), bodyStyle);
                        createCell(row, 4, formatDate(resolveEnrollmentDate(history, reportCard.getStudent())), centerBodyStyle);
                        createCell(row, 5, Boolean.TRUE.equals(history.getIsRepeater()) ? "Lưu ban" : "", bodyStyle);
                    }
                }

                rowIndex++;
                Row evaluationSectionRow = sheet.createRow(rowIndex++);
                createCell(evaluationSectionRow, 0, "ĐÁNH GIÁ NĂM HỌC", sectionStyle);
                sheet.addMergedRegion(new CellRangeAddress(evaluationSectionRow.getRowNum(), evaluationSectionRow.getRowNum(), 0, 5));

                Row evaluationHeaderRow = sheet.createRow(rowIndex++);
                String[] evaluationHeaders = { "Môn học/Hoạt động", "Giữa kỳ I", "Cuối kỳ I", "Giữa kỳ II", "Cuối kỳ II", "Nhận xét" };
                for (int i = 0; i < evaluationHeaders.length; i++) {
                    createCell(evaluationHeaderRow, i, evaluationHeaders[i], headerStyle);
                }
                if (reportCard.getEvaluationRows().isEmpty()) {
                    Row row = sheet.createRow(rowIndex++);
                    for (int i = 0; i < evaluationHeaders.length; i++) {
                        createCell(row, i, "", bodyStyle);
                    }
                } else {
                    for (ReportCardEvaluationRow evaluationRow : reportCard.getEvaluationRows()) {
                        Row row = sheet.createRow(rowIndex++);
                        createCell(row, 0, evaluationRow.getSubjectName(), bodyStyle);
                        createCell(row, 1, evaluationRow.getSemesterOneMidterm(), centerBodyStyle);
                        createCell(row, 2, evaluationRow.getSemesterOneFinal(), centerBodyStyle);
                        createCell(row, 3, evaluationRow.getSemesterTwoMidterm(), centerBodyStyle);
                        createCell(row, 4, evaluationRow.getSemesterTwoFinal(), centerBodyStyle);
                        createCell(row, 5, evaluationRow.getRemark(), bodyStyle);
                    }
                }

                rowIndex++;
                Row summarySectionRow = sheet.createRow(rowIndex++);
                createCell(summarySectionRow, 0, "TỔNG HỢP", sectionStyle);
                sheet.addMergedRegion(new CellRangeAddress(summarySectionRow.getRowNum(), summarySectionRow.getRowNum(), 0, 5));

                rowIndex = writeInfoRow(sheet, rowIndex, labelStyle, bodyStyle,
                        "Lớp hiện tại", reportCard.getLatestEnrollment().getClassroom() == null ? null
                                : reportCard.getLatestEnrollment().getClassroom().getName(),
                        "Ngày nhập học", formatDate(resolveEnrollmentDate(reportCard.getLatestEnrollment(), reportCard.getStudent())),
                        "Số buổi nghỉ có phép", String.valueOf(reportCard.getAttendanceSummary().getExcusedAbsences()));
                rowIndex = writeInfoRow(sheet, rowIndex, labelStyle, bodyStyle,
                        "Số buổi nghỉ không phép", String.valueOf(reportCard.getAttendanceSummary().getUnexcusedAbsences()),
                        "Kết luận", reportCard.getConclusion(),
                        "Diện chính sách", reportCard.getProfile() == null ? null : reportCard.getProfile().getPolicyObject());

                Row signRow = sheet.createRow(rowIndex + 1);
                createCell(signRow, 3, "Giáo viên chủ nhiệm", centerBodyStyle);
                createCell(signRow, 5, "Hiệu trưởng", centerBodyStyle);
            }

            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException ex) {
            throw new UserMessageException("Khong the xuat hoc ba Excel");
        }
    }

    private byte[] buildStudentReportCardPdf(List<StudentReportCardData> reportCards) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 28, 28, 24, 24);
            PdfWriter.getInstance(document, outputStream);
            document.open();

            com.lowagie.text.Font govFont = createPdfFont(11, com.lowagie.text.Font.BOLD);
            com.lowagie.text.Font titleFont = createPdfFont(16, com.lowagie.text.Font.BOLD);
            com.lowagie.text.Font sectionFont = createPdfFont(11, com.lowagie.text.Font.BOLD);
            com.lowagie.text.Font labelFont = createPdfFont(10, com.lowagie.text.Font.BOLD);
            com.lowagie.text.Font bodyFont = createPdfFont(10, com.lowagie.text.Font.NORMAL);
            com.lowagie.text.Font infoFont = createPdfFont(9, com.lowagie.text.Font.ITALIC);

            boolean firstStudent = true;
            for (StudentReportCardData reportCard : reportCards) {
                if (!firstStudent) {
                    document.newPage();
                }
                firstStudent = false;

                boolean renderSeparatedPages = true;
                if (renderSeparatedPages) {
                    addPdfCoverPageUtf8(document, reportCard, govFont, titleFont, labelFont, bodyFont, infoFont);

                    document.newPage();
                    addPdfStudentInfoPageUtf8(document, reportCard, sectionFont, labelFont, bodyFont, infoFont);

                    document.newPage();
                    addPdfLearningHistoryPageUtf8(document, reportCard, sectionFont, labelFont, bodyFont, infoFont);

                    for (ReportCardClassPage classPage : reportCard.getClassPages()) {
                        document.newPage();
                        addPdfExportInfoUtf8(document, infoFont);
                        addPdfClassEvaluationPageUtf8(document, reportCard, classPage, sectionFont, labelFont, bodyFont);
                    }
                    continue;
                }

                Paragraph info = new Paragraph(buildExportInfoLine(), infoFont);
                info.setAlignment(Element.ALIGN_RIGHT);
                info.setSpacingAfter(8f);
                document.add(info);

                Paragraph gov = new Paragraph("BỘ GIÁO DỤC VÀ ĐÀO TẠO", govFont);
                gov.setAlignment(Element.ALIGN_CENTER);
                document.add(gov);

                Paragraph school = new Paragraph(
                        reportCard.getStudent().getUnit() == null ? "" : reportCard.getStudent().getUnit().getName(),
                        bodyFont);
                school.setAlignment(Element.ALIGN_CENTER);
                school.setSpacingAfter(10f);
                document.add(school);

                Paragraph title = new Paragraph("HỌC BẠ TIỂU HỌC", titleFont);
                title.setAlignment(Element.ALIGN_CENTER);
                document.add(title);

                Paragraph studentName = new Paragraph(reportCard.getStudent().getFullName(), titleFont);
                studentName.setAlignment(Element.ALIGN_CENTER);
                studentName.setSpacingAfter(12f);
                document.add(studentName);
                document.newPage();
                addPdfExportInfo(document, infoFont);

                addPdfSectionTitle(document, "THÔNG TIN HỌC SINH", sectionFont);
                PdfPTable infoTable = new PdfPTable(new float[] { 1.4f, 2.6f, 1.4f, 2.6f });
                infoTable.setWidthPercentage(100);
                addPdfInfoPair(infoTable, "Họ và tên", reportCard.getStudent().getFullName(), labelFont, bodyFont);
                addPdfInfoPair(infoTable, "Mã học sinh", reportCard.getStudent().getStudentCode(), labelFont, bodyFont);
                addPdfInfoPair(infoTable, "Ngày sinh", formatDate(reportCard.getStudent().getDateOfBirth()), labelFont, bodyFont);
                addPdfInfoPair(infoTable, "Giới tính", studentGenderLabel(reportCard.getStudent().getGender()), labelFont, bodyFont);
                addPdfInfoPair(infoTable, "Dân tộc", reportCard.getStudent().getEthnicity(), labelFont, bodyFont);
                addPdfInfoPair(infoTable, "Quốc tịch", reportCard.getStudent().getNationality(), labelFont, bodyFont);
                addPdfInfoPair(infoTable, "Nơi sinh", reportCard.getStudent().getPlaceOfBirth(), labelFont, bodyFont);
                addPdfInfoPair(infoTable, "Địa chỉ", buildFullAddress(reportCard.getPermanentAddress()), labelFont, bodyFont);
                addPdfInfoPair(infoTable, "Cha", guardianSummary(reportCard.getFather()), labelFont, bodyFont);
                addPdfInfoPair(infoTable, "Mẹ", guardianSummary(reportCard.getMother()), labelFont, bodyFont);
                document.add(infoTable);

                addPdfSectionTitle(document, "QUÁ TRÌNH HỌC TẬP", sectionFont);
                PdfPTable historyTable = new PdfPTable(new float[] { 1.6f, 1.0f, 2.1f, 1.3f, 1.6f, 1.2f });
                historyTable.setWidthPercentage(100);
                String[] historyHeaders = { "Năm học", "Lớp", "Trường", "Sổ đăng bộ", "Ngày vào học/chuyển đến", "Ghi chú" };
                for (String header : historyHeaders) {
                    addPdfHeaderCell(historyTable, header, labelFont);
                }
                for (StudentEnrollment history : reportCard.getHistories()) {
                    addPdfBodyCell(historyTable, history.getSchoolYear() == null ? null : history.getSchoolYear().getName(), bodyFont,
                            Element.ALIGN_LEFT);
                    addPdfBodyCell(historyTable, history.getClassroom() == null ? null : history.getClassroom().getName(), bodyFont,
                            Element.ALIGN_LEFT);
                    addPdfBodyCell(historyTable,
                            reportCard.getStudent().getUnit() == null ? null : reportCard.getStudent().getUnit().getName(),
                            bodyFont, Element.ALIGN_LEFT);
                    addPdfBodyCell(historyTable, reportCard.getStudent().getBoardingBook(), bodyFont, Element.ALIGN_LEFT);
                    addPdfBodyCell(historyTable, formatDate(resolveEnrollmentDate(history, reportCard.getStudent())), bodyFont,
                            Element.ALIGN_CENTER);
                    addPdfBodyCell(historyTable, Boolean.TRUE.equals(history.getIsRepeater()) ? "Lưu ban" : "", bodyFont,
                            Element.ALIGN_LEFT);
                }
                document.add(historyTable);
                boolean splitEvaluationByClassPage = true;
                if (splitEvaluationByClassPage) {
                    for (ReportCardClassPage classPage : reportCard.getClassPages()) {
                        document.newPage();
                        addPdfExportInfo(document, infoFont);
                        addPdfClassEvaluationPage(document, reportCard, classPage, sectionFont, labelFont, bodyFont);
                    }
                    continue;
                }

                addPdfSectionTitle(document, "ĐÁNH GIÁ NĂM HỌC", sectionFont);
                PdfPTable evaluationTable = new PdfPTable(new float[] { 2.4f, 1.1f, 1.1f, 1.1f, 1.1f, 2.2f });
                evaluationTable.setWidthPercentage(100);
                String[] evaluationHeaders = { "Môn học/Hoạt động", "GK I", "CK I", "GK II", "CK II", "Nhận xét" };
                for (String header : evaluationHeaders) {
                    addPdfHeaderCell(evaluationTable, header, labelFont);
                }
                if (reportCard.getEvaluationRows().isEmpty()) {
                    for (int i = 0; i < evaluationHeaders.length; i++) {
                        addPdfBodyCell(evaluationTable, "", bodyFont, Element.ALIGN_LEFT);
                    }
                } else {
                    for (ReportCardEvaluationRow row : reportCard.getEvaluationRows()) {
                        addPdfBodyCell(evaluationTable, row.getSubjectName(), bodyFont, Element.ALIGN_LEFT);
                        addPdfBodyCell(evaluationTable, row.getSemesterOneMidterm(), bodyFont, Element.ALIGN_CENTER);
                        addPdfBodyCell(evaluationTable, row.getSemesterOneFinal(), bodyFont, Element.ALIGN_CENTER);
                        addPdfBodyCell(evaluationTable, row.getSemesterTwoMidterm(), bodyFont, Element.ALIGN_CENTER);
                        addPdfBodyCell(evaluationTable, row.getSemesterTwoFinal(), bodyFont, Element.ALIGN_CENTER);
                        addPdfBodyCell(evaluationTable, row.getRemark(), bodyFont, Element.ALIGN_LEFT);
                    }
                }
                document.add(evaluationTable);

                addPdfSectionTitle(document, "TỔNG HỢP", sectionFont);
                Paragraph summary = new Paragraph(
                        "Lớp hiện tại: "
                                + safeText(reportCard.getLatestEnrollment().getClassroom() == null ? null
                                        : reportCard.getLatestEnrollment().getClassroom().getName())
                                + "\nNgày nhập học: " + safeText(formatDate(resolveEnrollmentDate(reportCard.getLatestEnrollment(),
                                        reportCard.getStudent())))
                                + "\nSố buổi nghỉ có phép: " + reportCard.getAttendanceSummary().getExcusedAbsences()
                                + "\nSố buổi nghỉ không phép: " + reportCard.getAttendanceSummary().getUnexcusedAbsences()
                                + "\nKết luận: " + safeText(reportCard.getConclusion()),
                        bodyFont);
                summary.setSpacingAfter(14f);
                document.add(summary);
            }

            document.close();
            return outputStream.toByteArray();
        } catch (DocumentException | IOException ex) {
            throw new UserMessageException("Khong the xuat hoc ba PDF");
        }
    }

    private void addPdfCoverPage(Document document, StudentReportCardData reportCard, com.lowagie.text.Font govFont,
            com.lowagie.text.Font titleFont, com.lowagie.text.Font labelFont, com.lowagie.text.Font bodyFont,
            com.lowagie.text.Font infoFont) throws DocumentException {
        addPdfExportInfo(document, infoFont);

        PdfPTable coverTable = new PdfPTable(1);
        coverTable.setWidthPercentage(72);
        PdfPCell coverCell = new PdfPCell();
        coverCell.setPaddingTop(20f);
        coverCell.setPaddingBottom(28f);
        coverCell.setPaddingLeft(26f);
        coverCell.setPaddingRight(26f);
        coverCell.setMinimumHeight(700f);

        Paragraph gov = new Paragraph("BỘ GIÁO DỤC VÀ ĐÀO TẠO", govFont);
        gov.setAlignment(Element.ALIGN_CENTER);
        gov.setSpacingAfter(90f);
        coverCell.addElement(gov);

        Paragraph title = new Paragraph("HỌC BẠ\nTIỂU HỌC", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(180f);
        coverCell.addElement(title);

        PdfPTable metaTable = new PdfPTable(new float[] { 1.3f, 3.7f });
        metaTable.setWidthPercentage(100);
        addPdfInfoLine(metaTable, "Họ và tên học sinh", reportCard.getStudent().getFullName(), labelFont, bodyFont);
        addPdfInfoLine(metaTable, "Trường",
                reportCard.getStudent().getUnit() == null ? null : reportCard.getStudent().getUnit().getName(),
                labelFont, bodyFont);
        addPdfInfoLine(metaTable, "Xã (Phường, Thị trấn)", null, labelFont, bodyFont);
        addPdfInfoLine(metaTable, "Huyện (Thành phố, Quận, Thị xã)", null, labelFont, bodyFont);
        addPdfInfoLine(metaTable, "Tỉnh (Thành phố)", null, labelFont, bodyFont);
        coverCell.addElement(metaTable);

        coverTable.addCell(coverCell);
        document.add(coverTable);
    }

    private void addPdfStudentInfoPage(Document document, StudentReportCardData reportCard, com.lowagie.text.Font sectionFont,
            com.lowagie.text.Font labelFont, com.lowagie.text.Font bodyFont, com.lowagie.text.Font infoFont)
            throws DocumentException {
        addPdfExportInfo(document, infoFont);
        Paragraph title = new Paragraph("HỌC BẠ", sectionFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(12f);
        document.add(title);

        PdfPTable infoTable = new PdfPTable(new float[] { 1.3f, 2.2f, 1.0f, 1.5f });
        infoTable.setWidthPercentage(100);
        addPdfInfoPair(infoTable, "Họ và tên học sinh", reportCard.getStudent().getFullName(), labelFont, bodyFont);
        addPdfInfoPair(infoTable, "Giới tính", studentGenderLabel(reportCard.getStudent().getGender()), labelFont, bodyFont);
        addPdfInfoPair(infoTable, "Ngày, tháng, năm sinh", formatDate(reportCard.getStudent().getDateOfBirth()), labelFont, bodyFont);
        addPdfInfoPair(infoTable, "Dân tộc", reportCard.getStudent().getEthnicity(), labelFont, bodyFont);
        addPdfInfoPair(infoTable, "Nơi sinh", reportCard.getStudent().getPlaceOfBirth(), labelFont, bodyFont);
        addPdfInfoPair(infoTable, "Quốc tịch", reportCard.getStudent().getNationality(), labelFont, bodyFont);
        infoTable.setSpacingAfter(8f);
        document.add(infoTable);

        PdfPTable detailTable = new PdfPTable(1);
        detailTable.setWidthPercentage(100);
        addPdfInfoLine(detailTable, "Quê quán", buildHomeTown(reportCard.getPermanentAddress()), labelFont, bodyFont);
        addPdfInfoLine(detailTable, "Nơi ở hiện nay", buildFullAddress(reportCard.getPermanentAddress()), labelFont, bodyFont);
        addPdfInfoLine(detailTable, "Họ và tên cha", guardianName(reportCard.getFather()), labelFont, bodyFont);
        addPdfInfoLine(detailTable, "Họ và tên mẹ", guardianName(reportCard.getMother()), labelFont, bodyFont);
        addPdfInfoLine(detailTable, "Người giám hộ (nếu có)", guardianName(reportCard.getGuardian()), labelFont, bodyFont);
        detailTable.setSpacingAfter(28f);
        document.add(detailTable);

        PdfPTable signatureTable = new PdfPTable(1);
        signatureTable.setWidthPercentage(100);
        PdfPCell signatureCell = new PdfPCell();
        signatureCell.setBorder(PdfPCell.NO_BORDER);
        signatureCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        signatureCell.setPaddingTop(180f);

        Paragraph signatureText = new Paragraph(
                "......, ngày ...... tháng ...... năm 20....\nHIỆU TRƯỞNG\n(Ký, ghi rõ họ tên và đóng dấu)",
                bodyFont);
        signatureText.setAlignment(Element.ALIGN_RIGHT);
        signatureCell.addElement(signatureText);
        signatureTable.addCell(signatureCell);
        document.add(signatureTable);
    }

    private void addPdfLearningHistoryPage(Document document, StudentReportCardData reportCard, com.lowagie.text.Font sectionFont,
            com.lowagie.text.Font labelFont, com.lowagie.text.Font bodyFont, com.lowagie.text.Font infoFont)
            throws DocumentException {
        addPdfExportInfo(document, infoFont);
        addPdfSectionTitle(document, "QUÁ TRÌNH HỌC TẬP", sectionFont);

        PdfPTable historyTable = new PdfPTable(new float[] { 1.6f, 1.0f, 2.2f, 1.4f, 1.8f, 1.2f });
        historyTable.setWidthPercentage(100);
        historyTable.setHeaderRows(1);
        String[] historyHeaders = { "Năm học", "Lớp", "Tên trường", "Số đăng bộ", "Ngày nhập học/chuyển đến", "Ghi chú" };
        for (String header : historyHeaders) {
            addPdfHeaderCell(historyTable, header, labelFont);
        }

        if (reportCard.getHistories().isEmpty()) {
            for (int i = 0; i < historyHeaders.length; i++) {
                addPdfBodyCell(historyTable, "", bodyFont, Element.ALIGN_LEFT);
            }
        } else {
            for (StudentEnrollment history : reportCard.getHistories()) {
                addPdfBodyCell(historyTable, history.getSchoolYear() == null ? null : history.getSchoolYear().getName(), bodyFont,
                        Element.ALIGN_LEFT);
                addPdfBodyCell(historyTable, history.getClassroom() == null ? null : history.getClassroom().getName(), bodyFont,
                        Element.ALIGN_LEFT);
                addPdfBodyCell(historyTable,
                        reportCard.getStudent().getUnit() == null ? null : reportCard.getStudent().getUnit().getName(),
                        bodyFont, Element.ALIGN_LEFT);
                addPdfBodyCell(historyTable, reportCard.getStudent().getBoardingBook(), bodyFont, Element.ALIGN_LEFT);
                addPdfBodyCell(historyTable, formatDate(resolveEnrollmentDate(history, reportCard.getStudent())), bodyFont,
                        Element.ALIGN_CENTER);
                addPdfBodyCell(historyTable, Boolean.TRUE.equals(history.getIsRepeater()) ? "Lưu ban" : "", bodyFont,
                        Element.ALIGN_LEFT);
            }
        }
        document.add(historyTable);
    }

    private void addPdfClassEvaluationPage(Document document, StudentReportCardData reportCard, ReportCardClassPage classPage,
            com.lowagie.text.Font sectionFont, com.lowagie.text.Font labelFont, com.lowagie.text.Font bodyFont)
            throws DocumentException {
        String className = classPage.getEnrollment().getClassroom() == null ? null
                : classPage.getEnrollment().getClassroom().getName();

        PdfPTable metaTable = new PdfPTable(new float[] { 1.4f, 2.1f, 0.8f, 1.0f });
        metaTable.setWidthPercentage(100);
        addPdfInfoPair(metaTable, "Họ và tên học sinh", reportCard.getStudent().getFullName(), labelFont, bodyFont);
        addPdfInfoPair(metaTable, "Lớp", className, labelFont, bodyFont);
        addPdfInfoPair(metaTable, "Chiều cao", null, labelFont, bodyFont);
        addPdfInfoPair(metaTable, "Cân nặng", null, labelFont, bodyFont);
        addPdfInfoPair(metaTable, "Số ngày nghỉ có phép",
                String.valueOf(classPage.getAttendanceSummary().getExcusedAbsences()), labelFont, bodyFont);
        addPdfInfoPair(metaTable, "Số ngày nghỉ không phép",
                String.valueOf(classPage.getAttendanceSummary().getUnexcusedAbsences()), labelFont, bodyFont);
        metaTable.setSpacingAfter(8f);
        document.add(metaTable);

        Paragraph subjectTitle = new Paragraph("1. Các môn học và hoạt động giáo dục", labelFont);
        subjectTitle.setSpacingAfter(6f);
        document.add(subjectTitle);

        List<ReportCardEvaluationRow> orderedRows = orderReportCardRows(classPage.getEvaluationRows());
        PdfPTable evaluationTable = new PdfPTable(new float[] { 2.7f, 1.2f, 1.3f, 3.2f });
        evaluationTable.setWidthPercentage(100);
        String[] evaluationHeaders = { "Môn học và hoạt động giáo dục", "Mức đạt được", "Điểm KT ĐK", "Nhận xét" };
        for (String header : evaluationHeaders) {
            addPdfHeaderCell(evaluationTable, header, labelFont);
        }
        for (ReportCardEvaluationRow row : orderedRows) {
            addPdfBodyCell(evaluationTable, row.getSubjectName(), bodyFont, Element.ALIGN_LEFT);
            addPdfBodyCell(evaluationTable, buildReportCardLevel(row), bodyFont, Element.ALIGN_CENTER);
            addPdfBodyCell(evaluationTable, buildReportCardScore(row), bodyFont, Element.ALIGN_CENTER);
            addPdfBodyCell(evaluationTable, row.getRemark(), bodyFont, Element.ALIGN_LEFT);
        }
        evaluationTable.setSpacingAfter(10f);
        document.add(evaluationTable);

        Paragraph completionTitle = new Paragraph(
                "6. Hoàn thành chương trình lớp học/chương trình tiểu học: " + safeText(classPage.getConclusion()),
                labelFont);
        completionTitle.setSpacingAfter(8f);
        document.add(completionTitle);

        PdfPTable completionLines = new PdfPTable(1);
        completionLines.setWidthPercentage(100);
        addPdfBlankLine(completionLines, bodyFont);
        addPdfBlankLine(completionLines, bodyFont);
        completionLines.setSpacingAfter(8f);
        document.add(completionLines);

        Paragraph dateLine = new Paragraph(".........................., ngày .... tháng .... năm 20....", bodyFont);
        dateLine.setAlignment(Element.ALIGN_RIGHT);
        dateLine.setSpacingAfter(6f);
        document.add(dateLine);

        PdfPTable signatureTable = new PdfPTable(2);
        signatureTable.setWidthPercentage(100);
        signatureTable.setWidths(new float[] { 1f, 1f });
        addPdfSignatureCell(signatureTable, "Xác nhận của Hiệu trưởng", bodyFont);
        addPdfSignatureCell(signatureTable, "Giáo viên chủ nhiệm", bodyFont);
        document.add(signatureTable);
    }

    private void addPdfExportInfo(Document document, com.lowagie.text.Font infoFont) throws DocumentException {
        Paragraph info = new Paragraph(buildExportInfoLine(), infoFont);
        info.setAlignment(Element.ALIGN_RIGHT);
        info.setSpacingAfter(8f);
        document.add(info);
    }

    private void addPdfSignatureCell(PdfPTable table, String title, com.lowagie.text.Font font) {
        Paragraph text = new Paragraph(title + "\n\n\n(Ky va ghi ro ho ten)", font);
        text.setAlignment(Element.ALIGN_CENTER);
        PdfPCell cell = new PdfPCell(text);
        cell.setBorder(PdfPCell.NO_BORDER);
        cell.setPaddingTop(8f);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
    }

    private void addPdfInfoLine(PdfPTable table, String label, String value, com.lowagie.text.Font labelFont,
            com.lowagie.text.Font bodyFont) {
        PdfPCell labelCell = new PdfPCell(new Phrase(safeText(label) + ":", labelFont));
        labelCell.setBorder(PdfPCell.NO_BORDER);
        labelCell.setPaddingTop(8f);
        labelCell.setPaddingBottom(6f);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(safeText(value), bodyFont));
        valueCell.setBorder(PdfPCell.BOTTOM);
        valueCell.setPaddingTop(8f);
        valueCell.setPaddingBottom(6f);
        table.addCell(valueCell);
    }

    private void addPdfBlankLine(PdfPTable table, com.lowagie.text.Font bodyFont) {
        PdfPCell cell = new PdfPCell(new Phrase("", bodyFont));
        cell.setBorder(PdfPCell.BOTTOM);
        cell.setPaddingTop(8f);
        cell.setPaddingBottom(8f);
        table.addCell(cell);
    }

    private void addPdfCoverPageUtf8(Document document, StudentReportCardData reportCard, com.lowagie.text.Font govFont,
            com.lowagie.text.Font titleFont, com.lowagie.text.Font labelFont, com.lowagie.text.Font bodyFont,
            com.lowagie.text.Font infoFont) throws DocumentException {
        addPdfExportInfoUtf8(document, infoFont);

        PdfPTable coverTable = new PdfPTable(1);
        coverTable.setWidthPercentage(72);
        PdfPCell coverCell = new PdfPCell();
        coverCell.setPaddingTop(20f);
        coverCell.setPaddingBottom(28f);
        coverCell.setPaddingLeft(26f);
        coverCell.setPaddingRight(26f);
        coverCell.setMinimumHeight(700f);

        Paragraph gov = new Paragraph("BỘ GIÁO DỤC VÀ ĐÀO TẠO", govFont);
        gov.setAlignment(Element.ALIGN_CENTER);
        gov.setSpacingAfter(90f);
        coverCell.addElement(gov);

        Paragraph title = new Paragraph("HỌC BẠ\nTIỂU HỌC", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(180f);
        coverCell.addElement(title);

        PdfPTable metaTable = new PdfPTable(new float[] { 1.3f, 3.7f });
        metaTable.setWidthPercentage(100);
        addPdfInfoLine(metaTable, "Họ và tên học sinh", reportCard.getStudent().getFullName(), labelFont, bodyFont);
        addPdfInfoLine(metaTable, "Trường",
                reportCard.getStudent().getUnit() == null ? null : reportCard.getStudent().getUnit().getName(),
                labelFont, bodyFont);
        addPdfInfoLine(metaTable, "Xã (Phường, Thị trấn)", null, labelFont, bodyFont);
        addPdfInfoLine(metaTable, "Huyện (Thành phố, Quận, Thị xã)", null, labelFont, bodyFont);
        addPdfInfoLine(metaTable, "Tỉnh (Thành phố)", null, labelFont, bodyFont);
        coverCell.addElement(metaTable);

        coverTable.addCell(coverCell);
        document.add(coverTable);
    }

    private void addPdfStudentInfoPageUtf8(Document document, StudentReportCardData reportCard,
            com.lowagie.text.Font sectionFont, com.lowagie.text.Font labelFont, com.lowagie.text.Font bodyFont,
            com.lowagie.text.Font infoFont) throws DocumentException {
        addPdfExportInfoUtf8(document, infoFont);
        Paragraph title = new Paragraph("HỌC BẠ", sectionFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(12f);
        document.add(title);

        PdfPTable infoTable = new PdfPTable(new float[] { 1.3f, 2.2f, 1.0f, 1.5f });
        infoTable.setWidthPercentage(100);
        addPdfInfoPair(infoTable, "Họ và tên học sinh", reportCard.getStudent().getFullName(), labelFont, bodyFont);
        addPdfInfoPair(infoTable, "Giới tính", studentGenderLabel(reportCard.getStudent().getGender()), labelFont, bodyFont);
        addPdfInfoPair(infoTable, "Ngày, tháng, năm sinh", formatDate(reportCard.getStudent().getDateOfBirth()), labelFont,
                bodyFont);
        addPdfInfoPair(infoTable, "Dân tộc", reportCard.getStudent().getEthnicity(), labelFont, bodyFont);
        addPdfInfoPair(infoTable, "Nơi sinh", reportCard.getStudent().getPlaceOfBirth(), labelFont, bodyFont);
        addPdfInfoPair(infoTable, "Quốc tịch", reportCard.getStudent().getNationality(), labelFont, bodyFont);
        infoTable.setSpacingAfter(8f);
        document.add(infoTable);

        PdfPTable detailTable = new PdfPTable(1);
        detailTable.setWidthPercentage(100);
        addPdfInfoLine(detailTable, "Quê quán", buildHomeTown(reportCard.getPermanentAddress()), labelFont, bodyFont);
        addPdfInfoLine(detailTable, "Nơi ở hiện nay", buildFullAddress(reportCard.getPermanentAddress()), labelFont, bodyFont);
        addPdfInfoLine(detailTable, "Họ và tên cha", guardianName(reportCard.getFather()), labelFont, bodyFont);
        addPdfInfoLine(detailTable, "Họ và tên mẹ", guardianName(reportCard.getMother()), labelFont, bodyFont);
        addPdfInfoLine(detailTable, "Người giám hộ (nếu có)", guardianName(reportCard.getGuardian()), labelFont, bodyFont);
        detailTable.setSpacingAfter(28f);
        document.add(detailTable);

        PdfPTable signatureTable = new PdfPTable(1);
        signatureTable.setWidthPercentage(100);
        PdfPCell signatureCell = new PdfPCell();
        signatureCell.setBorder(PdfPCell.NO_BORDER);
        signatureCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        signatureCell.setPaddingTop(180f);

        Paragraph signatureText = new Paragraph(
                "......, ngày ...... tháng ...... năm 20....\nHIỆU TRƯỞNG\n(Ký, ghi rõ họ tên và đóng dấu)",
                bodyFont);
        signatureText.setAlignment(Element.ALIGN_RIGHT);
        signatureCell.addElement(signatureText);
        signatureTable.addCell(signatureCell);
        document.add(signatureTable);
    }

    private void addPdfLearningHistoryPageUtf8(Document document, StudentReportCardData reportCard,
            com.lowagie.text.Font sectionFont, com.lowagie.text.Font labelFont, com.lowagie.text.Font bodyFont,
            com.lowagie.text.Font infoFont) throws DocumentException {
        addPdfExportInfoUtf8(document, infoFont);
        addPdfSectionTitle(document, "QUÁ TRÌNH HỌC TẬP", sectionFont);

        PdfPTable historyTable = new PdfPTable(new float[] { 1.6f, 1.0f, 2.2f, 1.4f, 1.8f, 1.2f });
        historyTable.setWidthPercentage(100);
        historyTable.setHeaderRows(1);
        String[] historyHeaders = { "Năm học", "Lớp", "Tên trường", "Số đăng bộ", "Ngày nhập học/chuyển đến", "Ghi chú" };
        for (String header : historyHeaders) {
            addPdfHeaderCell(historyTable, header, labelFont);
        }

        if (reportCard.getHistories().isEmpty()) {
            for (int i = 0; i < historyHeaders.length; i++) {
                addPdfBodyCell(historyTable, "", bodyFont, Element.ALIGN_LEFT);
            }
        } else {
            for (StudentEnrollment history : reportCard.getHistories()) {
                addPdfBodyCell(historyTable, history.getSchoolYear() == null ? null : history.getSchoolYear().getName(), bodyFont,
                        Element.ALIGN_LEFT);
                addPdfBodyCell(historyTable, history.getClassroom() == null ? null : history.getClassroom().getName(), bodyFont,
                        Element.ALIGN_LEFT);
                addPdfBodyCell(historyTable,
                        reportCard.getStudent().getUnit() == null ? null : reportCard.getStudent().getUnit().getName(),
                        bodyFont, Element.ALIGN_LEFT);
                addPdfBodyCell(historyTable, reportCard.getStudent().getBoardingBook(), bodyFont, Element.ALIGN_LEFT);
                addPdfBodyCell(historyTable, formatDate(resolveEnrollmentDate(history, reportCard.getStudent())), bodyFont,
                        Element.ALIGN_CENTER);
                addPdfBodyCell(historyTable, Boolean.TRUE.equals(history.getIsRepeater()) ? "Lưu ban" : "", bodyFont,
                        Element.ALIGN_LEFT);
            }
        }
        document.add(historyTable);
    }

    private void addPdfClassEvaluationPageUtf8(Document document, StudentReportCardData reportCard,
            ReportCardClassPage classPage, com.lowagie.text.Font sectionFont, com.lowagie.text.Font labelFont,
            com.lowagie.text.Font bodyFont) throws DocumentException {
        String className = classPage.getEnrollment().getClassroom() == null ? null
                : classPage.getEnrollment().getClassroom().getName();

        PdfPTable metaTable = new PdfPTable(new float[] { 1.4f, 2.1f, 0.8f, 1.0f });
        metaTable.setWidthPercentage(100);
        addPdfInfoPair(metaTable, "Họ và tên học sinh", reportCard.getStudent().getFullName(), labelFont, bodyFont);
        addPdfInfoPair(metaTable, "Lớp", className, labelFont, bodyFont);
        addPdfInfoPair(metaTable, "Chiều cao", null, labelFont, bodyFont);
        addPdfInfoPair(metaTable, "Cân nặng", null, labelFont, bodyFont);
        addPdfInfoPair(metaTable, "Số ngày nghỉ có phép",
                String.valueOf(classPage.getAttendanceSummary().getExcusedAbsences()), labelFont, bodyFont);
        addPdfInfoPair(metaTable, "Số ngày nghỉ không phép",
                String.valueOf(classPage.getAttendanceSummary().getUnexcusedAbsences()), labelFont, bodyFont);
        metaTable.setSpacingAfter(8f);
        document.add(metaTable);

        Paragraph subjectTitle = new Paragraph("1. Các môn học và hoạt động giáo dục", labelFont);
        subjectTitle.setSpacingAfter(6f);
        document.add(subjectTitle);

        List<ReportCardEvaluationRow> orderedRows = orderReportCardRows(classPage.getEvaluationRows());
        PdfPTable evaluationTable = new PdfPTable(new float[] { 2.7f, 1.2f, 1.3f, 3.2f });
        evaluationTable.setWidthPercentage(100);
        String[] evaluationHeaders = { "Môn học và hoạt động giáo dục", "Mức đạt được", "Điểm KT ĐK", "Nhận xét" };
        for (String header : evaluationHeaders) {
            addPdfHeaderCell(evaluationTable, header, labelFont);
        }
        for (ReportCardEvaluationRow row : orderedRows) {
            addPdfBodyCell(evaluationTable, row.getSubjectName(), bodyFont, Element.ALIGN_LEFT);
            addPdfBodyCell(evaluationTable, buildReportCardLevel(row), bodyFont, Element.ALIGN_CENTER);
            addPdfBodyCell(evaluationTable, buildReportCardScore(row), bodyFont, Element.ALIGN_CENTER);
            addPdfBodyCell(evaluationTable, row.getRemark(), bodyFont, Element.ALIGN_LEFT);
        }
        evaluationTable.setSpacingAfter(10f);
        document.add(evaluationTable);

        Paragraph completionTitle = new Paragraph(
                "6. Hoàn thành chương trình lớp học/chương trình tiểu học: " + safeText(classPage.getConclusion()),
                labelFont);
        completionTitle.setSpacingAfter(8f);
        document.add(completionTitle);

        PdfPTable completionLines = new PdfPTable(1);
        completionLines.setWidthPercentage(100);
        addPdfBlankLine(completionLines, bodyFont);
        addPdfBlankLine(completionLines, bodyFont);
        completionLines.setSpacingAfter(8f);
        document.add(completionLines);

        Paragraph dateLine = new Paragraph(".........................., ngày .... tháng .... năm 20....", bodyFont);
        dateLine.setAlignment(Element.ALIGN_RIGHT);
        dateLine.setSpacingAfter(6f);
        document.add(dateLine);

        PdfPTable signatureTable = new PdfPTable(2);
        signatureTable.setWidthPercentage(100);
        signatureTable.setWidths(new float[] { 1f, 1f });
        addPdfSignatureCellUtf8(signatureTable, "Xác nhận của Hiệu trưởng", bodyFont);
        addPdfSignatureCellUtf8(signatureTable, "Giáo viên chủ nhiệm", bodyFont);
        document.add(signatureTable);
    }

    private void addPdfExportInfoUtf8(Document document, com.lowagie.text.Font infoFont) throws DocumentException {
        Paragraph info = new Paragraph(buildExportInfoLineUtf8(), infoFont);
        info.setAlignment(Element.ALIGN_RIGHT);
        info.setSpacingAfter(8f);
        document.add(info);
    }

    private void addPdfSignatureCellUtf8(PdfPTable table, String title, com.lowagie.text.Font font) {
        Paragraph text = new Paragraph(title + "\n\n\n(Ký và ghi rõ họ tên)", font);
        text.setAlignment(Element.ALIGN_CENTER);
        PdfPCell cell = new PdfPCell(text);
        cell.setBorder(PdfPCell.NO_BORDER);
        cell.setPaddingTop(8f);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
    }

    private String buildExportInfoLineUtf8() {
        String exportTime = LocalDateTime.now().format(EXPORT_TIME_FORMATTER);
        String username = SecurityUtils.getCurrentUsername();
        return "Thời gian tải: " + exportTime + " | Người tải: " + username;
    }

    private void upsertStudentFromExcelRow(Long unitId, Row row, DataFormatter formatter) {
        String fullName = normalizeNullable(readCellText(row.getCell(STUDENT_COL_FULL_NAME), formatter));
        if (!StringUtils.hasText(fullName)) {
            throw new UserMessageException("Họ tên học sinh không được để trống");
        }

        StudentCreateRequest request = new StudentCreateRequest();
        request.setStudentCode(null);
        request.setFullName(fullName);
        request.setFirstName(normalizeNullable(readCellText(row.getCell(STUDENT_COL_FIRST_NAME), formatter)));
        request.setDateOfBirth(
                parseDateCell(readCellText(row.getCell(STUDENT_COL_DOB), formatter), "Ngày sinh không hợp lệ"));
        request.setGender(parseStudentGenderCell(readCellText(row.getCell(STUDENT_COL_GENDER), formatter)));
        request.setMobilePhone(normalizeNullable(readCellText(row.getCell(STUDENT_COL_PHONE), formatter)));
        request.setEmail(normalizeNullable(readCellText(row.getCell(STUDENT_COL_EMAIL), formatter)));
        request.setMoeCode(normalizeNullable(readCellText(row.getCell(STUDENT_COL_MOE_CODE), formatter)));
        request.setPlaceOfBirth(normalizeNullable(readCellText(row.getCell(STUDENT_COL_PLACE_OF_BIRTH), formatter)));
        request.setEthnicity(normalizeNullable(readCellText(row.getCell(STUDENT_COL_ETHNICITY), formatter)));
        request.setReligion(normalizeNullable(readCellText(row.getCell(STUDENT_COL_RELIGION), formatter)));
        request.setNationality(normalizeNullable(readCellText(row.getCell(STUDENT_COL_NATIONALITY), formatter)));
        request.setIdentityNumber(normalizeNullable(readCellText(row.getCell(STUDENT_COL_IDENTITY_NUMBER), formatter)));
        request.setIdentityIssueDate(
                parseOptionalDateCell(readCellText(row.getCell(STUDENT_COL_IDENTITY_ISSUE_DATE), formatter)));
        request.setIdentityIssuePlace(
                normalizeNullable(readCellText(row.getCell(STUDENT_COL_IDENTITY_ISSUE_PLACE), formatter)));
        request.setHealthInsuranceNumber(
                normalizeNullable(readCellText(row.getCell(STUDENT_COL_HEALTH_INSURANCE_NO), formatter)));
        request.setBloodGroup(normalizeNullable(readCellText(row.getCell(STUDENT_COL_BLOOD_GROUP), formatter)));
        request.setBoardingBook(normalizeNullable(readCellText(row.getCell(STUDENT_COL_BOARDING_BOOK), formatter)));
        request.setAdmissionType(parseIntegerCell(readCellText(row.getCell(STUDENT_COL_ADMISSION_TYPE), formatter),
                "Loại nhập học không hợp lệ"));
        request.setUnitId(unitId);
        request.setAdmissionDate(
                parseOptionalDateCell(readCellText(row.getCell(STUDENT_COL_ADMISSION_DATE), formatter)));
        request.setStudentStatus(
                parseStudentStatusCell(readCellText(row.getCell(STUDENT_COL_STUDENT_STATUS), formatter)));

        String schoolYearName = readCellText(row.getCell(STUDENT_COL_SCHOOL_YEAR_ID), formatter);
        SchoolYear schoolYear = findSchoolYearByName(schoolYearName);
        String className = readCellText(row.getCell(STUDENT_COL_CLASS_ID), formatter);
        Classroom classroom = findClassroomByName(unitId, schoolYear.getId(), className);

        StudentEnrollmentCreateRequest enrollment = new StudentEnrollmentCreateRequest();
        enrollment.setSchoolYearId(schoolYear.getId());
        enrollment.setClassId(classroom.getId());
        enrollment
                .setEnrolledAt(parseOptionalDateCell(readCellText(row.getCell(STUDENT_COL_ADMISSION_DATE), formatter)));
        enrollment.setStatus(parseIntegerCell(readCellText(row.getCell(STUDENT_COL_ENROLLMENT_STATUS), formatter),
                "Trạng thái nhập học không hợp lệ"));
        enrollment.setIsRepeater(parseOptionalBooleanCell(
                readCellText(row.getCell(STUDENT_COL_ENROLLMENT_IS_REPEATER), formatter), "Ưu tiên không hợp lệ"));
        enrollment.setSessionsPerWeek(parseIntegerCell(
                readCellText(row.getCell(STUDENT_COL_ENROLLMENT_SESSIONS), formatter), "Số buổi tuần không hợp lệ"));
        enrollment.setStudyMode(parseIntegerCell(
                readCellText(row.getCell(STUDENT_COL_ENROLLMENT_STUDY_MODE), formatter), "Hình thức học không hợp lệ"));
        enrollment.setIsBoarding(parseOptionalBooleanCell(
                readCellText(row.getCell(STUDENT_COL_ENROLLMENT_BOARDING), formatter), "Bán trú không hợp lệ"));
        enrollment.setIsTwoSessionsPerDay(
                parseOptionalBooleanCell(readCellText(row.getCell(STUDENT_COL_ENROLLMENT_TWO_SESSIONS), formatter),
                        "Học 2 buổi/ngày không hợp lệ"));
        request.setEnrollment(enrollment);

        StudentAddressCreateRequest permanentAddress = buildPermanentAddressRequest(row, formatter);
        if (permanentAddress != null) {
            request.setAddresses(List.of(permanentAddress));
        }

        List<StudentGuardianCreateRequest> guardians = new ArrayList<>();
        StudentGuardianCreateRequest father = buildGuardianRequest(
                GUARDIAN_TYPE_FATHER,
                readCellText(row.getCell(STUDENT_COL_FATHER_NAME), formatter),
                readCellText(row.getCell(STUDENT_COL_FATHER_BIRTH_YEAR), formatter),
                readCellText(row.getCell(STUDENT_COL_FATHER_OCCUPATION), formatter),
                readCellText(row.getCell(STUDENT_COL_FATHER_PHONE), formatter),
                readCellText(row.getCell(STUDENT_COL_FATHER_EMAIL), formatter),
                readCellText(row.getCell(STUDENT_COL_FATHER_IDENTITY), formatter),
                readCellText(row.getCell(STUDENT_COL_FATHER_IS_ETHNIC), formatter));
        if (father != null) {
            guardians.add(father);
        }
        StudentGuardianCreateRequest mother = buildGuardianRequest(
                GUARDIAN_TYPE_MOTHER,
                readCellText(row.getCell(STUDENT_COL_MOTHER_NAME), formatter),
                readCellText(row.getCell(STUDENT_COL_MOTHER_BIRTH_YEAR), formatter),
                readCellText(row.getCell(STUDENT_COL_MOTHER_OCCUPATION), formatter),
                readCellText(row.getCell(STUDENT_COL_MOTHER_PHONE), formatter),
                readCellText(row.getCell(STUDENT_COL_MOTHER_EMAIL), formatter),
                readCellText(row.getCell(STUDENT_COL_MOTHER_IDENTITY), formatter),
                readCellText(row.getCell(STUDENT_COL_MOTHER_IS_ETHNIC), formatter));
        if (mother != null) {
            guardians.add(mother);
        }
        if (!guardians.isEmpty()) {
            request.setGuardians(guardians);
        }

        StudentProfileCreateRequest profile = buildProfileRequest(row, formatter);
        if (profile != null) {
            request.setProfile(profile);
        }

        create(request);
    }

    private byte[] buildStudentImportErrorFile(Workbook workbook, Sheet sheet, Map<Integer, String> rowErrors) {
        CellStyle headerStyle = createExcelHeaderStyle(workbook);
        CellStyle errorStyle = createExcelErrorBodyStyle(workbook);

        Row headerRow = sheet.getRow(STUDENT_IMPORT_HEADER_ROW_INDEX);
        if (headerRow == null) {
            headerRow = sheet.createRow(STUDENT_IMPORT_HEADER_ROW_INDEX);
        }

        int resultColumnIndex = headerRow.getLastCellNum() < 0
                ? STUDENT_IMPORT_LAST_DATA_COLUMN + 1
                : headerRow.getLastCellNum();
        int reasonColumnIndex = resultColumnIndex + 1;

        createCell(headerRow, resultColumnIndex, "Kết quả", headerStyle);
        createCell(headerRow, reasonColumnIndex, "Lý do lỗi", headerStyle);

        for (Map.Entry<Integer, String> entry : rowErrors.entrySet()) {
            Row row = sheet.getRow(entry.getKey());
            if (row == null) {
                row = sheet.createRow(entry.getKey());
            }
            applyRowStyle(row, 0, reasonColumnIndex, errorStyle);
            createCell(row, resultColumnIndex, "Thất bại", errorStyle);
            createCell(row, reasonColumnIndex, entry.getValue(), errorStyle);
        }

        autosize(sheet, reasonColumnIndex + 1);

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException ex) {
            throw new UserMessageException("Không thể tạo file lỗi import học sinh");
        }
    }

    private void validateExcelFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new UserMessageException("File import khong duoc de trong");
        }
    }

    private boolean isExcelRowEmpty(Row row, int fromColumn, int toColumn, DataFormatter formatter) {
        if (row == null) {
            return true;
        }
        for (int i = fromColumn; i <= toColumn; i++) {
            if (StringUtils.hasText(readCellText(row.getCell(i), formatter))) {
                return false;
            }
        }
        return true;
    }

    private String readCellText(Cell cell, DataFormatter formatter) {
        return cell == null ? "" : formatter.formatCellValue(cell).trim();
    }

    private java.time.LocalDate parseDateCell(String value, String message) {
        java.time.LocalDate parsed = parseOptionalDateCell(value);
        if (parsed == null) {
            throw new UserMessageException(message);
        }
        return parsed;
    }

    private java.time.LocalDate parseOptionalDateCell(String value) {
        String normalized = normalizeNullable(value);
        if (normalized == null) {
            return null;
        }
        try {
            if (normalized.contains("/")) {
                return java.time.LocalDate.parse(normalized, DATE_FORMATTER);
            }
            return java.time.LocalDate.parse(normalized);
        } catch (Exception ex) {
            throw new UserMessageException("Ngay thang khong hop le: " + normalized);
        }
    }

    private Integer parseIntegerCell(String value, String message) {
        String normalized = normalizeNullable(value);
        if (normalized == null) {
            return null;
        }
        try {
            return Integer.valueOf(normalized);
        } catch (NumberFormatException ex) {
            throw new UserMessageException(message);
        }
    }

    private Integer parseStudentGenderCell(String value) {
        String normalized = normalizeNullable(value);
        if (normalized == null) {
            return null;
        }
        String key = normalizeLookupKey(normalized);
        return switch (key) {
            case "NAM", "MALE", "M", "0" -> 0;
            case "NU", "FEMALE", "F", "1" -> 1;
            default -> parseIntegerCell(normalized, "Giới tính không hợp lệ");
        };
    }

    private Integer parseStudentStatusCell(String value) {
        String normalized = normalizeNullable(value);
        if (normalized == null) {
            return null;
        }
        String key = normalizeLookupKey(normalized);
        return switch (key) {
            case "DANG HOC", "DANGHOC", "STUDYING", "0" -> 0;
            case "DA CHUYEN TRUONG", "DA CHUYEN", "CHUYEN TRUONG", "TRANSFERRED", "1" -> 1;
            case "TAM NGHI", "TAMNGHI", "ON LEAVE", "LEAVE", "2" -> 2;
            case "THOI HOC", "THOIHOC", "DROPPED OUT", "DROPOUT", "3" -> 3;
            default -> parseIntegerCell(normalized, "Trạng thái học sinh không hợp lệ");
        };
    }

    private String studentGenderLabel(Integer gender) {
        if (gender == null) {
            return null;
        }
        return switch (gender) {
            case 0 -> "Nam";
            case 1 -> "Nữ";
            default -> String.valueOf(gender);
        };
    }

    private String studentStatusLabel(Integer status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case 0 -> "Đang học";
            case 1 -> "Đã chuyển trường";
            case 2 -> "Tạm nghỉ";
            case 3 -> "Thôi học";
            default -> String.valueOf(status);
        };
    }

    private Boolean parseOptionalBooleanCell(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        String key = normalizeLookupKey(value);
        return switch (key) {
            case "X", "TRUE", "YES", "Y", "CO", "1" -> Boolean.TRUE;
            case "FALSE", "NO", "N", "KHONG", "0" -> Boolean.FALSE;
            default -> throw new UserMessageException(message);
        };
    }

    private String normalizeLookupKey(String value) {
        String normalized = normalizeNullable(value);
        if (normalized == null) {
            return null;
        }
        String noAccent = Normalizer.normalize(normalized, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replace('đ', 'd')
                .replace('Đ', 'D');
        return noAccent.trim().toUpperCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    private StudentAddressCreateRequest buildPermanentAddressRequest(Row row, DataFormatter formatter) {
        String province = normalizeNullable(readCellText(row.getCell(STUDENT_COL_ADDR_PROVINCE), formatter));
        String ward = normalizeNullable(readCellText(row.getCell(STUDENT_COL_ADDR_WARD), formatter));
        String hamlet = normalizeNullable(readCellText(row.getCell(STUDENT_COL_ADDR_HAMLET), formatter));
        String detail = normalizeNullable(readCellText(row.getCell(STUDENT_COL_ADDR_DETAIL), formatter));
        if (province == null && ward == null && hamlet == null && detail == null) {
            return null;
        }
        StudentAddressCreateRequest address = new StudentAddressCreateRequest();
        address.setAddressType(ADDRESS_TYPE_PERMANENT);
        address.setProvinceName(province);
        address.setWardName(ward);
        address.setHamletName(hamlet);
        address.setDetailAddress(detail);
        return address;
    }

    private StudentGuardianCreateRequest buildGuardianRequest(String guardianType, String fullName, String birthYear,
            String occupation, String phone, String email, String identityNumber, String isEthnicText) {
        StudentGuardianCreateRequest guardian = new StudentGuardianCreateRequest();
        guardian.setGuardianType(guardianType);
        guardian.setFullName(normalizeNullable(fullName));
        guardian.setBirthYear(parseIntegerCell(birthYear, "Năm sinh của người giám hộ không hợp lệ"));
        guardian.setOccupation(normalizeNullable(occupation));
        guardian.setPhone(normalizeNullable(phone));
        guardian.setEmail(normalizeNullable(email));
        guardian.setIdentityNumber(normalizeNullable(identityNumber));
        guardian.setIsEthnic(parseGuardianEthnicCell(isEthnicText));

        boolean hasData = guardian.getFullName() != null
                || guardian.getBirthYear() != null
                || guardian.getOccupation() != null
                || guardian.getPhone() != null
                || guardian.getEmail() != null
                || guardian.getIdentityNumber() != null
                || guardian.getIsEthnic() != null;
        if (!hasData) {
            return null;
        }
        if (guardian.getIsEthnic() == null) {
            guardian.setIsEthnic(Boolean.FALSE);
        }
        return guardian;
    }

    private Boolean parseGuardianEthnicCell(String value) {
        String normalized = normalizeNullable(value);
        if (normalized == null) {
            return null;
        }
        String key = normalizeLookupKey(normalized);
        return !"KINH".equals(key);
    }

    private StudentProfileCreateRequest buildProfileRequest(Row row, DataFormatter formatter) {
        StudentProfileCreateRequest profile = new StudentProfileCreateRequest();
        profile.setPolicyObject(
                normalizeNullable(readCellText(row.getCell(STUDENT_COL_PROFILE_POLICY_OBJECT), formatter)));
        profile.setPolicyBenefit(
                normalizeNullable(readCellText(row.getCell(STUDENT_COL_PROFILE_POLICY_BENEFIT), formatter)));
        profile.setPriorityCategory(
                normalizeNullable(readCellText(row.getCell(STUDENT_COL_PROFILE_PRIORITY_CATEGORY), formatter)));
        profile.setRegionCategory(
                normalizeNullable(readCellText(row.getCell(STUDENT_COL_PROFILE_REGION_CATEGORY), formatter)));
        profile.setDisabilityType(
                normalizeNullable(readCellText(row.getCell(STUDENT_COL_PROFILE_DISABILITY_TYPE), formatter)));
        profile.setDisabilityExemptEval(parseOptionalBooleanCell(
                readCellText(row.getCell(STUDENT_COL_PROFILE_DISABILITY_EXEMPT), formatter),
                "Miễn đánh giá khuyết tật không hợp lệ"));
        profile.setSupportTuitionCost(parseOptionalBooleanCell(
                readCellText(row.getCell(STUDENT_COL_PROFILE_SUPPORT_TUITION), formatter),
                "Hỗ trợ chi phí không hợp lệ"));
        profile.setHasParentInternet(parseOptionalBooleanCell(
                readCellText(row.getCell(STUDENT_COL_PROFILE_PARENT_INTERNET), formatter),
                "Thông tin internet tại nhà không hợp lệ"));
        profile.setHasParentSmartphone(parseOptionalBooleanCell(
                readCellText(row.getCell(STUDENT_COL_PROFILE_PARENT_SMARTPHONE), formatter),
                "Thông tin smartphone cha/me không hợp lệ"));
        profile.setOtherSystemCode(
                normalizeNullable(readCellText(row.getCell(STUDENT_COL_PROFILE_OTHER_SYSTEM_CODE), formatter)));
        profile.setSsoCode(normalizeNullable(readCellText(row.getCell(STUDENT_COL_PROFILE_SSO_CODE), formatter)));

        boolean hasData = profile.getPolicyObject() != null
                || profile.getPolicyBenefit() != null
                || profile.getPriorityCategory() != null
                || profile.getRegionCategory() != null
                || profile.getDisabilityType() != null
                || profile.getDisabilityExemptEval() != null
                || profile.getSupportTuitionCost() != null
                || profile.getHasParentInternet() != null
                || profile.getHasParentSmartphone() != null
                || profile.getOtherSystemCode() != null
                || profile.getSsoCode() != null;
        return hasData ? profile : null;
    }

    private int writeInfoRow(Sheet sheet, int rowIndex, CellStyle labelStyle, CellStyle valueStyle,
            String labelOne, String valueOne,
            String labelTwo, String valueTwo,
            String labelThree, String valueThree) {
        Row row = sheet.createRow(rowIndex);
        createCell(row, 0, labelOne, labelStyle);
        createCell(row, 1, valueOne, valueStyle);
        createCell(row, 2, labelTwo, labelStyle);
        createCell(row, 3, valueTwo, valueStyle);
        createCell(row, 4, labelThree, labelStyle);
        createCell(row, 5, valueThree, valueStyle);
        return rowIndex + 1;
    }

    private LocalDate resolveEnrollmentDate(StudentEnrollment enrollment, Student student) {
        if (enrollment != null && enrollment.getEnrolledAt() != null) {
            return enrollment.getEnrolledAt();
        }
        return student == null ? null : student.getAdmissionDate();
    }

    private StudentAddress findPermanentAddress(List<StudentAddress> addresses) {
        for (StudentAddress address : safeList(addresses)) {
            if (address != null && ADDRESS_TYPE_PERMANENT.equals(address.getAddressType())) {
                return address;
            }
        }
        return safeList(addresses).stream().findFirst().orElse(null);
    }

    private StudentGuardian findGuardianByType(List<StudentGuardian> guardians, String guardianType) {
        for (StudentGuardian guardian : safeList(guardians)) {
            if (guardian != null && guardianType.equals(guardian.getGuardianType())) {
                return guardian;
            }
        }
        return null;
    }

    private String buildFullAddress(StudentAddress address) {
        if (address == null) {
            return null;
        }
        List<String> parts = new ArrayList<>();
        if (StringUtils.hasText(address.getDetailAddress())) {
            parts.add(address.getDetailAddress().trim());
        }
        if (StringUtils.hasText(address.getHamletName())) {
            parts.add(address.getHamletName().trim());
        }
        if (StringUtils.hasText(address.getWardName())) {
            parts.add(address.getWardName().trim());
        }
        if (StringUtils.hasText(address.getProvinceName())) {
            parts.add(address.getProvinceName().trim());
        }
        return parts.isEmpty() ? null : String.join(", ", parts);
    }

    private String guardianSummary(StudentGuardian guardian) {
        if (guardian == null) {
            return null;
        }
        List<String> parts = new ArrayList<>();
        if (StringUtils.hasText(guardian.getFullName())) {
            parts.add(guardian.getFullName().trim());
        }
        if (StringUtils.hasText(guardian.getPhone())) {
            parts.add(guardian.getPhone().trim());
        }
        if (guardian.getBirthYear() != null) {
            parts.add(String.valueOf(guardian.getBirthYear()));
        }
        return parts.isEmpty() ? null : String.join(" - ", parts);
    }

    private String guardianName(StudentGuardian guardian) {
        if (guardian == null || !StringUtils.hasText(guardian.getFullName())) {
            return null;
        }
        return guardian.getFullName().trim();
    }

    private String buildHomeTown(StudentAddress address) {
        if (address == null) {
            return null;
        }
        List<String> parts = new ArrayList<>();
        if (StringUtils.hasText(address.getWardName())) {
            parts.add(address.getWardName().trim());
        }
        if (StringUtils.hasText(address.getProvinceName())) {
            parts.add(address.getProvinceName().trim());
        }
        return parts.isEmpty() ? null : String.join(", ", parts);
    }

    private String formatEvaluationValue(StudentEvaluation evaluation, boolean midterm) {
        if (evaluation == null) {
            return null;
        }
        String level = midterm ? evaluation.getMidtermLevel() : evaluation.getFinalLevel();
        if (StringUtils.hasText(level)) {
            return level.trim();
        }
        Double score = midterm ? evaluation.getMidtermScore() : evaluation.getFinalScore();
        if (score == null) {
            return null;
        }
        if (score.doubleValue() == Math.rint(score.doubleValue())) {
            return String.valueOf(score.intValue());
        }
        return String.format(Locale.ROOT, "%.1f", score);
    }

    private String buildEvaluationRemark(StudentEvaluation semesterOne, StudentEvaluation semesterTwo) {
        LinkedHashSet<String> remarks = new LinkedHashSet<>();
        if (semesterOne != null) {
            if (StringUtils.hasText(semesterOne.getMidtermRemark())) {
                remarks.add(semesterOne.getMidtermRemark().trim());
            }
            if (StringUtils.hasText(semesterOne.getFinalRemark())) {
                remarks.add(semesterOne.getFinalRemark().trim());
            }
        }
        if (semesterTwo != null) {
            if (StringUtils.hasText(semesterTwo.getMidtermRemark())) {
                remarks.add(semesterTwo.getMidtermRemark().trim());
            }
            if (StringUtils.hasText(semesterTwo.getFinalRemark())) {
                remarks.add(semesterTwo.getFinalRemark().trim());
            }
        }
        return remarks.isEmpty() ? null : String.join("; ", remarks);
    }

    private List<ReportCardEvaluationRow> orderReportCardRows(List<ReportCardEvaluationRow> rows) {
        Map<String, ReportCardEvaluationRow> byNormalizedName = new LinkedHashMap<>();
        for (ReportCardEvaluationRow row : safeList(rows)) {
            if (row == null || !StringUtils.hasText(row.getSubjectName())) {
                continue;
            }
            byNormalizedName.put(normalizeSubjectName(row.getSubjectName()), row);
        }

        List<ReportCardEvaluationRow> orderedRows = new ArrayList<>();
        for (String templateSubject : REPORT_CARD_SUBJECT_TEMPLATE) {
            ReportCardEvaluationRow matchedRow = byNormalizedName.remove(normalizeSubjectName(templateSubject));
            orderedRows.add(matchedRow == null
                    ? new ReportCardEvaluationRow(templateSubject, null, null, null, null, null)
                    : new ReportCardEvaluationRow(
                            templateSubject,
                            matchedRow.getSemesterOneMidterm(),
                            matchedRow.getSemesterOneFinal(),
                            matchedRow.getSemesterTwoMidterm(),
                            matchedRow.getSemesterTwoFinal(),
                            matchedRow.getRemark()));
        }
        orderedRows.addAll(byNormalizedName.values());
        return orderedRows;
    }

    private String normalizeSubjectName(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ")
                .trim();

        if (normalized.startsWith("ngoai ngu")) {
            return "ngoai ngu 1";
        }
        if (normalized.contains("tin hoc") && normalized.contains("cong nghe")) {
            if (normalized.contains("tin hoc") && normalized.endsWith("tin hoc")) {
                return "tin hoc va cong nghe tin hoc";
            }
            if (normalized.contains("cong nghe")) {
                return "tin hoc va cong nghe cong nghe";
            }
        }
        if (normalized.contains("nghe thuat") && normalized.contains("am nhac")) {
            return "nghe thuat am nhac";
        }
        if (normalized.contains("nghe thuat") && (normalized.contains("mi thuat") || normalized.contains("my thuat"))) {
            return "nghe thuat mi thuat";
        }
        return normalized;
    }

    private String buildReportCardLevel(ReportCardEvaluationRow row) {
        return firstNonBlank(
                row.getSemesterTwoFinal(),
                row.getSemesterTwoMidterm(),
                row.getSemesterOneFinal(),
                row.getSemesterOneMidterm());
    }

    private String buildReportCardScore(ReportCardEvaluationRow row) {
        List<String> parts = new ArrayList<>();
        appendScorePart(parts, "GK I", row.getSemesterOneMidterm());
        appendScorePart(parts, "CK I", row.getSemesterOneFinal());
        appendScorePart(parts, "GK II", row.getSemesterTwoMidterm());
        appendScorePart(parts, "CK II", row.getSemesterTwoFinal());
        return parts.isEmpty() ? null : String.join(" | ", parts);
    }

    private void appendScorePart(List<String> parts, String label, String value) {
        if (StringUtils.hasText(value)) {
            parts.add(label + ": " + value.trim());
        }
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private String buildReportCardSheetName(String fullName, int order) {
        String rawName = StringUtils.hasText(fullName) ? fullName.trim() : "HocBa-" + order;
        String sanitized = rawName.replaceAll("[\\\\/*?:\\[\\]]", "-");
        if (sanitized.length() > 31) {
            return sanitized.substring(0, 31);
        }
        return sanitized;
    }

    private void addPdfSectionTitle(Document document, String title, com.lowagie.text.Font font) throws DocumentException {
        Paragraph paragraph = new Paragraph(title, font);
        paragraph.setSpacingBefore(10f);
        paragraph.setSpacingAfter(6f);
        document.add(paragraph);
    }

    private void addPdfInfoPair(PdfPTable table, String label, String value, com.lowagie.text.Font labelFont,
            com.lowagie.text.Font bodyFont) {
        PdfPCell labelCell = new PdfPCell(new Phrase(safeText(label), labelFont));
        labelCell.setPadding(5f);
        labelCell.setBackgroundColor(new java.awt.Color(245, 247, 250));
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(safeText(value), bodyFont));
        valueCell.setPadding(5f);
        table.addCell(valueCell);
    }

    private String safeText(String value) {
        return value == null ? "" : value;
    }

    private String formatDate(java.time.LocalDate value) {
        return value == null ? null : value.format(DATE_FORMATTER);
    }

    private String buildExportInfoLine() {
        String exportTime = LocalDateTime.now().format(EXPORT_TIME_FORMATTER);
        String username = SecurityUtils.getCurrentUsername();
        return "Thời gian tải: " + exportTime + " | Người tải: " + username;
    }

    private void fillRowWithStyle(Row row, int fromColumn, int toColumn, CellStyle style) {
        for (int i = fromColumn; i <= toColumn; i++) {
            createCell(row, i, "", style);
        }
    }

    private void applyRowStyle(Row row, int fromColumn, int toColumn, CellStyle style) {
        for (int i = fromColumn; i <= toColumn; i++) {
            Cell cell = row.getCell(i);
            if (cell == null) {
                cell = row.createCell(i);
                cell.setCellValue("");
            }
            cell.setCellStyle(style);
        }
    }

    private CellStyle createExcelHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor((short) 41);
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setWrapText(true);
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontName(EXPORT_FONT_NAME);
        style.setFont(font);
        return style;
    }

    private CellStyle createExcelTitleStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 16);
        font.setFontName(EXPORT_FONT_NAME);
        style.setFont(font);
        return style;
    }

    private CellStyle createExcelGovHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontName(EXPORT_FONT_NAME);
        style.setFont(font);
        return style;
    }

    private CellStyle createExcelGovSubHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        Font font = workbook.createFont();
        font.setBold(true);
        font.setUnderline(Font.U_SINGLE);
        font.setFontName(EXPORT_FONT_NAME);
        style.setFont(font);
        return style;
    }

    private CellStyle createExcelBodyStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setVerticalAlignment(VerticalAlignment.TOP);
        style.setWrapText(true);
        Font font = workbook.createFont();
        font.setFontName(EXPORT_FONT_NAME);
        style.setFont(font);
        return style;
    }

    private CellStyle createExcelErrorBodyStyle(Workbook workbook) {
        CellStyle style = createExcelBodyStyle(workbook);
        Font font = workbook.createFont();
        font.setFontName(EXPORT_FONT_NAME);
        font.setColor(IndexedColors.RED.getIndex());
        style.setFont(font);
        return style;
    }

    private void addPdfHeaderCell(PdfPTable table, String text, com.lowagie.text.Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text == null ? "" : text, font));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(6f);
        cell.setBackgroundColor(new java.awt.Color(224, 242, 241));
        table.addCell(cell);
    }

    private void addPdfBodyCell(PdfPTable table, String text, com.lowagie.text.Font font, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text == null ? "" : text, font));
        cell.setHorizontalAlignment(align);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(5f);
        table.addCell(cell);
    }

    private com.lowagie.text.Font createPdfFont(float size, int style) {
        String fontPath = switch (style) {
            case com.lowagie.text.Font.BOLD -> TIMES_FONT_BOLD_PATH;
            case com.lowagie.text.Font.ITALIC -> TIMES_FONT_ITALIC_PATH;
            default -> TIMES_FONT_REGULAR_PATH;
        };
        try {
            if (Files.exists(java.nio.file.Path.of(fontPath))) {
                BaseFont baseFont = BaseFont.createFont(fontPath, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
                return new com.lowagie.text.Font(baseFont, size, style);
            }
        } catch (Exception ignored) {
        }
        return com.lowagie.text.FontFactory.getFont(EXPORT_FONT_NAME, BaseFont.IDENTITY_H, true, size, style);
    }

    private CellStyle createExcelGuideStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        Font font = workbook.createFont();
        font.setFontHeightInPoints((short) 10);
        font.setItalic(true);
        font.setFontName(EXPORT_FONT_NAME);
        style.setFont(font);
        style.setWrapText(true);
        return style;
    }

    private CellStyle createExcelInfoStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.RIGHT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        Font font = workbook.createFont();
        font.setFontHeightInPoints((short) 10);
        font.setItalic(true);
        font.setFontName(EXPORT_FONT_NAME);
        style.setFont(font);
        return style;
    }

    private Cell createCell(Row row, int columnIndex, Object value, CellStyle style) {
        Cell cell = row.createCell(columnIndex);
        if (value instanceof Number number) {
            cell.setCellValue(number.doubleValue());
        } else if (value != null) {
            cell.setCellValue(String.valueOf(value));
        } else {
            cell.setCellValue("");
        }
        if (style != null) {
            cell.setCellStyle(style);
        }
        return cell;
    }

    private void autosize(Sheet sheet, int totalColumns) {
        for (int i = 0; i < totalColumns; i++) {
            sheet.autoSizeColumn(i);
            sheet.setColumnWidth(i, Math.min(sheet.getColumnWidth(i) + 1024, 20000));
        }
    }

    private String booleanMark(Boolean value) {
        return Boolean.TRUE.equals(value) ? "X" : "";
    }

    private CellStyle createExcelSectionStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setFillForegroundColor((short) 22);
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontName(EXPORT_FONT_NAME);
        style.setFont(font);
        return style;
    }

    private CellStyle createExcelLabelCellStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setWrapText(true);
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontName(EXPORT_FONT_NAME);
        style.setFont(font);
        return style;
    }

    private CellStyle createExcelCenteredBodyStyle(Workbook workbook) {
        CellStyle style = createExcelBodyStyle(workbook);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    // ─── Inner record types for Report Card export ───────────────────────────

    private record StudentReportCardData(
            Student student,
            StudentEnrollment latestEnrollment,
            List<StudentEnrollment> histories,
            StudentAddress permanentAddress,
            StudentGuardian father,
            StudentGuardian mother,
            StudentGuardian guardian,
            StudentProfile profile,
            List<ReportCardEvaluationRow> evaluationRows,
            AttendanceSummary attendanceSummary,
            String conclusion,
            List<ReportCardClassPage> classPages) {

        public Student getStudent() { return student; }
        public StudentEnrollment getLatestEnrollment() { return latestEnrollment; }
        public List<StudentEnrollment> getHistories() { return histories; }
        public StudentAddress getPermanentAddress() { return permanentAddress; }
        public StudentGuardian getFather() { return father; }
        public StudentGuardian getMother() { return mother; }
        public StudentGuardian getGuardian() { return guardian; }
        public StudentProfile getProfile() { return profile; }
        public List<ReportCardEvaluationRow> getEvaluationRows() { return evaluationRows; }
        public AttendanceSummary getAttendanceSummary() { return attendanceSummary; }
        public String getConclusion() { return conclusion; }
        public List<ReportCardClassPage> getClassPages() { return classPages; }
    }

    private record ReportCardClassPage(
            StudentEnrollment enrollment,
            List<ReportCardEvaluationRow> evaluationRows,
            AttendanceSummary attendanceSummary,
            String conclusion) {

        public StudentEnrollment getEnrollment() { return enrollment; }
        public List<ReportCardEvaluationRow> getEvaluationRows() { return evaluationRows; }
        public AttendanceSummary getAttendanceSummary() { return attendanceSummary; }
        public String getConclusion() { return conclusion; }
    }

    private record ReportCardEvaluationRow(
            String subjectName,
            String semesterOneMidterm,
            String semesterOneFinal,
            String semesterTwoMidterm,
            String semesterTwoFinal,
            String remark) {

        public String getSubjectName() { return subjectName; }
        public String getSemesterOneMidterm() { return semesterOneMidterm; }
        public String getSemesterOneFinal() { return semesterOneFinal; }
        public String getSemesterTwoMidterm() { return semesterTwoMidterm; }
        public String getSemesterTwoFinal() { return semesterTwoFinal; }
        public String getRemark() { return remark; }
    }

    private record AttendanceSummary(int excusedAbsences, int unexcusedAbsences) {
        public int getExcusedAbsences() { return excusedAbsences; }
        public int getUnexcusedAbsences() { return unexcusedAbsences; }
    }
}
