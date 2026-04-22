package com.gfi.backend.services.implement;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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
import com.gfi.backend.models.dtos.student.StudentProfileCreateRequest;
import com.gfi.backend.models.dtos.student.StudentProfileItemDto;
import com.gfi.backend.models.entities.Classroom;
import com.gfi.backend.models.entities.SchoolYear;
import com.gfi.backend.models.entities.Student;
import com.gfi.backend.models.entities.StudentAddress;
import com.gfi.backend.models.entities.StudentEnrollment;
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
import com.gfi.backend.repositories.SchoolYearRepository;
import com.gfi.backend.repositories.StudentAddressRepository;
import com.gfi.backend.repositories.StudentEnrollmentRepository;
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
    private static final String FEATURE = FeatureKey.STUDENT_PROFILE.getCode();
    private static final String EXPORT_FONT_NAME = "Times New Roman";
    private static final String EXCEL_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter EXPORT_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final String TIMES_FONT_REGULAR_PATH = "C:/Windows/Fonts/times.ttf";
    private static final String TIMES_FONT_BOLD_PATH = "C:/Windows/Fonts/timesbd.ttf";
    private static final String TIMES_FONT_ITALIC_PATH = "C:/Windows/Fonts/timesi.ttf";
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
                        .name(student.getStudentCode() + " - " + student.getFullName())
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
            sheet.addMergedRegion(new CellRangeAddress(6, 6, STUDENT_COL_PROFILE_POLICY_OBJECT, STUDENT_COL_PROFILE_SSO_CODE));

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

    private Long parseLongCell(String value, String message) {
        String normalized = normalizeNullable(value);
        if (normalized == null) {
            throw new UserMessageException(message);
        }
        try {
            return Long.valueOf(normalized);
        } catch (NumberFormatException ex) {
            throw new UserMessageException(message);
        }
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
                return new com.lowagie.text.Font(baseFont, size, com.lowagie.text.Font.NORMAL);
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
}
