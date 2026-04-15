package com.gfi.backend.services.implement;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gfi.backend.controllers.exceptions.UserMessageException;
import com.gfi.backend.models.dtos.common.PageRequestDto;
import com.gfi.backend.models.dtos.common.PageResponseDto;
import com.gfi.backend.models.dtos.student.StudentAddressCreateRequest;
import com.gfi.backend.models.dtos.student.StudentAddressItemDto;
import com.gfi.backend.models.dtos.student.StudentCreateRequest;
import com.gfi.backend.models.dtos.student.StudentEnrollmentCreateRequest;
import com.gfi.backend.models.dtos.student.StudentEnrollmentItemDto;
import com.gfi.backend.models.dtos.student.StudentFilterDto;
import com.gfi.backend.models.dtos.student.StudentGuardianCreateRequest;
import com.gfi.backend.models.dtos.student.StudentGuardianItemDto;
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
import com.gfi.backend.services.interfaces.StudentCodeGeneratorService;
import com.gfi.backend.services.interfaces.StudentService;
import com.gfi.backend.utils.PageableUtils;

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
    private static final String FEATURE = FeatureKey.STUDENT.getCode();

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

    private Classroom findClassroom(Long id) {
        return classroomRepository.findById(id)
                .orElseThrow(() -> new UserMessageException(CommonErrorCode.CLASS_NOT_FOUND));
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
                .gradeLevelId(enrollment.getClassroom() == null || enrollment.getClassroom().getGradeLevel() == null ? null : enrollment.getClassroom().getGradeLevel().getId())
                .gradeLevelName(enrollment.getClassroom() == null || enrollment.getClassroom().getGradeLevel() == null ? null : enrollment.getClassroom().getGradeLevel().getName())
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
}
