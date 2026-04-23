package com.gfi.backend.services.implement;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.gfi.backend.models.dtos.staff.*;
import com.gfi.backend.models.dtos.common.PageRequestDto;
import com.gfi.backend.models.dtos.common.PageResponseDto;
import com.gfi.backend.models.dtos.common.TemporaryFileDto;
import com.gfi.backend.models.entities.Staff;
import com.gfi.backend.models.entities.StaffAddress;
import com.gfi.backend.models.entities.StaffEducation;
import com.gfi.backend.models.entities.StaffFamilyMember;
import com.gfi.backend.models.entities.Unit;
import com.gfi.backend.models.entities.GradeLevel;
import com.gfi.backend.models.global.CommonErrorCode;
import com.gfi.backend.controllers.exceptions.UserMessageException;
import com.gfi.backend.repositories.StaffAddressRepository;
import com.gfi.backend.repositories.StaffEducationRepository;
import com.gfi.backend.repositories.StaffFamilyMemberRepository;
import com.gfi.backend.repositories.StaffRepository;
import com.gfi.backend.repositories.UnitRepository;
import com.gfi.backend.repositories.UserRepository;
import com.gfi.backend.models.enums.ActionType;
import com.gfi.backend.models.enums.ExportType;
import com.gfi.backend.models.enums.ScopeType;
import com.gfi.backend.models.security.ResolvedScope;
import com.gfi.backend.services.FileStorageService;
import com.gfi.backend.services.interfaces.ImportErrorFileStorageService;
import com.gfi.backend.services.interfaces.StaffCodeGeneratorService;
import com.gfi.backend.services.interfaces.StaffService;
import com.gfi.backend.utils.PageableUtils;
import com.gfi.backend.utils.ScopeFilterUtils;
import com.gfi.backend.utils.SecurityContextUtils;
import com.gfi.backend.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jakarta.persistence.criteria.Predicate;
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

@Service
@RequiredArgsConstructor
@Slf4j
public class StaffServiceImpl implements StaffService {
    private static final String ADDRESS_TYPE_PERMANENT = "PERMANENT";
    private static final String ADDRESS_TYPE_TEMPORARY = "TEMPORARY";
    private static final String ADDRESS_TYPE_BIRTH_PLACE = "BIRTH_PLACE";
    private static final String RELATION_FATHER = "FATHER";
    private static final String RELATION_MOTHER = "MOTHER";
    private static final String RELATION_SPOUSE = "SPOUSE";
    private static final String RELATION_SPOUSE_FATHER = "SPOUSE_FATHER";
    private static final String RELATION_SPOUSE_MOTHER = "SPOUSE_MOTHER";
    private static final String RELATION_CHILDREN = "CHILDREN";
    private static final String EDUCATION_TYPE_TRAINING = "TRAINING";
    private static final String EDUCATION_TYPE_FOREIGN_LANGUAGE = "FOREIGN_LANGUAGE";
    private static final List<String> STAFF_ETHNICITY_OPTIONS = List.of(
            "Kinh", "Tày", "Thái", "Hoa", "Khơ-me", "Mường", "Nùng", "HMông", "Dao", "Gia-rai",
            "Ngái", "Ê-đê", "Ba na", "Xơ-Đăng", "Sán Chay", "Cơ-ho", "Chăm", "Sán Dìu", "Hrê", "Mnông",
            "Ra-glai", "Xtiêng", "Bru-Vân Kiều", "Thổ", "Giáy", "Cơ-tu", "Gié Triêng", "Mạ", "Khơ-mú", "Co",
            "Tà-ôi", "Chơ-ro", "Kháng", "Xinh-mun", "Hà Nhì", "Chu ru", "Lào", "La Chí", "La Ha", "Phù Lá",
            "La Hủ", "Lự", "Lô Lô", "Chứt", "Mảng", "Pà Thẻn", "Co Lao", "Cống", "Bố Y", "Si La",
            "Pu Péo", "Brâu", "Ơ Đu", "Rơ măm", "Người nước ngoài", "Không rõ");

    private final StaffRepository staffRepository;
    private final UnitRepository unitRepository;
    private final com.gfi.backend.repositories.GradeLevelRepository gradeLevelRepository;
    private final StaffAddressRepository staffAddressRepository;
    private final StaffEducationRepository staffEducationRepository;
    private final StaffFamilyMemberRepository staffFamilyMemberRepository;
    private final FileStorageService fileStorageService;
    private final UserRepository userRepository;
    private final ImportErrorFileStorageService importErrorFileStorageService;
    private final StaffCodeGeneratorService staffCodeGeneratorService;

    private static final String FEATURE = "STAFF_PROFILE";
    private static final String EXCEL_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final String EXPORT_FONT_NAME = "Times New Roman";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter EXPORT_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final String TIMES_FONT_REGULAR_PATH = "C:/Windows/Fonts/times.ttf";
    private static final String TIMES_FONT_BOLD_PATH = "C:/Windows/Fonts/timesbd.ttf";
    private static final String TIMES_FONT_ITALIC_PATH = "C:/Windows/Fonts/timesi.ttf";
    private static final int STAFF_TEMPLATE_GROUP_HEADER_ROW_INDEX = 6;
    private static final int STAFF_IMPORT_HEADER_ROW_INDEX = 7;
    private static final int STAFF_TEMPLATE_SAMPLE_ROW_INDEX = 8;
    private static final int STAFF_LEGACY_CODE_COLUMN_INDEX = 1;
    private static final int STAFF_COL_FULL_NAME = 1;
    private static final int STAFF_COL_ALIAS_NAME = 2;
    private static final int STAFF_COL_GENDER = 3;
    private static final int STAFF_COL_DOB = 4;
    private static final int STAFF_COL_PHONE = 5;
    private static final int STAFF_COL_EMAIL = 6;
    private static final int STAFF_COL_GRADE_ID = 7;
    private static final int STAFF_COL_STATUS = 8;
    private static final int STAFF_COL_NOTE = 9;
    private static final int STAFF_COL_IDENTITY_CODE = 10;
    private static final int STAFF_COL_ETHNICITY = 11;
    private static final int STAFF_COL_RELIGION = 12;
    private static final int STAFF_COL_NATIONALITY = 13;
    private static final int STAFF_COL_CCCD_NO = 14;
    private static final int STAFF_COL_CCCD_ISSUE_DATE = 15;
    private static final int STAFF_COL_CCCD_ISSUE_PLACE = 16;
    private static final int STAFF_COL_HEALTH_STATUS = 17;
    private static final int STAFF_COL_SOCIAL_INSURANCE_NO = 18;
    private static final int STAFF_COL_PERMANENT_ADDRESS = 19;
    private static final int STAFF_COL_TEMPORARY_ADDRESS = 20;
    private static final int STAFF_COL_BIRTH_PLACE_ADDRESS = 21;
    private static final int STAFF_COL_FATHER_NAME = 22;
    private static final int STAFF_COL_FATHER_BIRTH_YEAR = 23;
    private static final int STAFF_COL_FATHER_HOMETOWN = 24;
    private static final int STAFF_COL_FATHER_OCCUPATION = 25;
    private static final int STAFF_COL_FATHER_PHONE = 26;
    private static final int STAFF_COL_MOTHER_NAME = 27;
    private static final int STAFF_COL_MOTHER_BIRTH_YEAR = 28;
    private static final int STAFF_COL_MOTHER_HOMETOWN = 29;
    private static final int STAFF_COL_MOTHER_OCCUPATION = 30;
    private static final int STAFF_COL_MOTHER_PHONE = 31;
    private static final int STAFF_COL_SPOUSE_NAME = 32;
    private static final int STAFF_COL_SPOUSE_BIRTH_YEAR = 33;
    private static final int STAFF_COL_SPOUSE_OCCUPATION = 34;
    private static final int STAFF_COL_SPOUSE_PHONE = 35;
    private static final int STAFF_COL_SPOUSE_FATHER_NAME = 36;
    private static final int STAFF_COL_SPOUSE_FATHER_BIRTH_YEAR = 37;
    private static final int STAFF_COL_SPOUSE_FATHER_HOMETOWN = 38;
    private static final int STAFF_COL_SPOUSE_FATHER_OCCUPATION = 39;
    private static final int STAFF_COL_SPOUSE_FATHER_PHONE = 40;
    private static final int STAFF_COL_SPOUSE_MOTHER_NAME = 41;
    private static final int STAFF_COL_SPOUSE_MOTHER_BIRTH_YEAR = 42;
    private static final int STAFF_COL_SPOUSE_MOTHER_HOMETOWN = 43;
    private static final int STAFF_COL_SPOUSE_MOTHER_OCCUPATION = 44;
    private static final int STAFF_COL_SPOUSE_MOTHER_PHONE = 45;
    private static final int STAFF_COL_CHILDREN_DETAIL = 46;
    private static final int STAFF_COL_TRAINING_SCHOOL = 47;
    private static final int STAFF_COL_TRAINING_MAJOR = 48;
    private static final int STAFF_COL_TRAINING_FORM = 49;
    private static final int STAFF_COL_TRAINING_CERTIFICATE = 50;
    private static final int STAFF_COL_TRAINING_FROM_DATE = 51;
    private static final int STAFF_COL_TRAINING_TO_DATE = 52;
    private static final int STAFF_COL_TRAINING_NOTE = 53;
    private static final int STAFF_COL_FOREIGN_LANGUAGE_NAME = 54;
    private static final int STAFF_COL_FOREIGN_LANGUAGE_LEVEL = 55;
    private static final int STAFF_COL_FOREIGN_LANGUAGE_ISSUE_DATE = 56;
    private static final int STAFF_COL_FOREIGN_LANGUAGE_SCORE = 57;
    private static final int STAFF_COL_FOREIGN_LANGUAGE_NOTE = 58;
    private static final int STAFF_IMPORT_LAST_DATA_COLUMN = STAFF_COL_FOREIGN_LANGUAGE_NOTE;

    /*
     * Tìm kiếm cán bộ
     * Gồm các thông tin cơ bản để hiển thị trong danh sách, không bao gồm thông tin
     * chi tiết như địa chỉ, thành viên gia đình
     */
    @Override
    @Transactional(readOnly = true)
    public PageResponseDto<StaffItemDto, StaffFilterDto> search(PageRequestDto<StaffFilterDto> request) {
        StaffFilterDto filter = request.getFilter() == null ? new StaffFilterDto() : request.getFilter();
        Long currentStaffId = resolveCurrentStaffIdForSelfScope(ActionType.VIEW);
        List<ResolvedScope> resolvedScopes = ScopeFilterUtils.getScopesForQuery(FEATURE, ActionType.VIEW);
        int pageSize = normalizePageSize(request.getPageSize());
        int pageNow = normalizePageNow(request.getPageNow());
        Pageable pageable = PageableUtils.newestFirst(pageNow, pageSize);

        Page<Staff> page = staffRepository.findAll(buildSpecification(filter, currentStaffId, resolvedScopes),
                pageable);
        List<StaffItemDto> items = page.getContent().stream()
                .map(this::toItemDto)
                .toList();

        return PageResponseDto.<StaffItemDto, StaffFilterDto>builder()
                .pageSize(pageSize)
                .pageNow(pageNow)
                .filter(filter)
                .pageTotal(page.getTotalPages())
                .recordTotal(page.getTotalElements())
                .items(items)
                .build();
    }

    /*
     * Lấy thông tin chi tiết cán bộ theo ID
     */
    @Override
    @Transactional(readOnly = true)
    public StaffDetailDto getById(Long id) {
        Staff staff = findStaff(id);
        validateStaffAccess(staff, ActionType.VIEW);
        return toDetailDto(staff);
    }

    /*
     * Tạo mới cán bộ
     * Bao gồm thông tin cơ bản và chi tiết như địa chỉ, thành viên gia đình
     * Kiểm tra trùng mã cán bộ, nếu có sẽ báo lỗi
     * Kiểm tra tồn tại đơn vị, nếu không có sẽ báo lỗi
     */
    @Override
    @Transactional
    public StaffDetailDto create(StaffCreateRequest request) {
        // Validate staff code
        String staffCode = normalize(request.getStaffCode());
        staffRepository.findByStaffCode(staffCode)
                .ifPresent(s -> {
                    throw new UserMessageException(CommonErrorCode.STAFF_CODE_ALREADY_EXISTS);
                });

        // Validate unit exists
        Unit unit = unitRepository.findById(request.getUnitId())
                .orElseThrow(() -> new UserMessageException(CommonErrorCode.UNIT_NOT_FOUND));

        GradeLevel gradeLevel = null;
        if (request.getGradeId() != null) {
            gradeLevel = gradeLevelRepository.findById(request.getGradeId())
                    .orElseThrow(() -> new UserMessageException(CommonErrorCode.GRADE_LEVEL_NOT_FOUND));
        }

        Staff staff = new Staff();
        applyStaffFields(staff, request);
        staff.setStaffCode(staffCode);
        staff.setUnit(unit);
        staff.setGradeLevel(gradeLevel);
        applyMediaFields(staff, unit, request.getAvatarUrl(), request.getSignatureUrl());
        staff.setStatus(request.getStatus() != null ? request.getStatus() : "ACTIVE");
        staff.setCreatedBy(SecurityUtils.getCurrentUsername());
        staff.setDeletedFlag(0);

        Staff saved = staffRepository.save(staff);
        List<StaffAddress> addresses = replaceAddresses(saved, request);
        List<StaffFamilyMember> familyMembers = replaceFamilyMembers(saved, request);
        return toDetailDto(saved, addresses, familyMembers);
    }

    /*
     * Cập nhật thông tin cán bộ
     * Cho phép cập nhật thông tin cơ bản và chi tiết như địa chỉ, thành viên gia
     * đình
     * Kiểm tra tồn tại cán bộ, nếu không có sẽ báo lỗi
     * Kiểm tra tồn tại đơn vị nếu có thay đổi, nếu không có sẽ báo lỗi
     * Kiểm tra tồn tại chức vụ nếu có thay đổi, nếu không có sẽ báo lỗi
     * Kiểm tra trùng mã cán bộ nếu có thay đổi, nếu có sẽ báo lỗi
     */
    @Override
    @Transactional
    public StaffDetailDto update(Long id, StaffUpdateRequest request) {
        Staff staff = findStaff(id);
        validateStaffAccess(staff, ActionType.EDIT);
        if (request.getUnitId() != null
                && (staff.getUnit() == null || !request.getUnitId().equals(staff.getUnit().getId()))) {
            Unit unit = unitRepository.findById(request.getUnitId())
                    .orElseThrow(() -> new UserMessageException(CommonErrorCode.UNIT_NOT_FOUND));
            staff.setUnit(unit);
        }
        if (request.getGradeId() != null) {
            GradeLevel gradeLevel = gradeLevelRepository.findById(request.getGradeId())
                    .orElseThrow(() -> new UserMessageException(CommonErrorCode.GRADE_LEVEL_NOT_FOUND));
            if (staff.getGradeLevel() == null || !request.getGradeId().equals(staff.getGradeLevel().getId())) {
                staff.setGradeLevel(gradeLevel);
            }
        }
        applyStaffFields(staff, request);
        applyMediaFields(staff, staff.getUnit(), request.getAvatarUrl(), request.getSignatureUrl());
        staff.setUpdatedBy(SecurityUtils.getCurrentUsername());

        Staff saved = staffRepository.save(staff);
        List<StaffAddress> addresses = replaceAddresses(saved, request);
        List<StaffFamilyMember> familyMembers = replaceFamilyMembers(saved, request);
        return toDetailDto(saved, addresses, familyMembers);
    }

    /*
     * Xóa cán bộ
     * Thực hiện xóa mềm bằng cách set deletedFlag = 1, không xóa bản ghi thực tế
     * trong database để đảm bảo tính toàn vẹn dữ liệu liên quan như địa chỉ, thành
     * viên gia đình
     * Kiểm tra tồn tại cán bộ, nếu không có sẽ báo lỗi
     */
    @Override
    @Transactional
    public void delete(Long id) {
        Staff staff = findStaff(id);
        validateStaffAccess(staff, ActionType.DELETE);
        staffAddressRepository.deleteByStaffId(id);
        staffFamilyMemberRepository.deleteByStaffId(id);
        staff.setDeletedFlag(1);
        staff.setDeletedBy(SecurityUtils.getCurrentUsername());
        staffRepository.save(staff);
    }

    /*
     * Lấy danh sách cán bộ theo chức vụ và đơn vị
     * Nếu unitId được cung cấp, sẽ lọc theo cả chức vụ và đơn vị
     * Nếu unitId không được cung cấp, sẽ lọc theo chức vụ trên toàn bộ hệ thống
     * Thông tin trả về chỉ bao gồm id, mã cán bộ, họ tên, id đơn vị, id chức vụ để
     * hiển thị trong dropdown chọn cán bộ theo chức vụ
     */
    @Override
    @Transactional(readOnly = true)
    public List<StaffGradeItemDto> getByGrade(Long gradeLevelId, Long unitId) {
        List<Staff> staffs;
        if (unitId != null) {
            staffs = staffRepository.findByUnitIdAndGradeLevelId(unitId, gradeLevelId);
        } else {
            staffs = staffRepository.findByGradeLevelId(gradeLevelId);
        }
        return staffs.stream().map(this::toGradeItemDto).toList();
    }

    /*
     * Xây dựng điều kiện lọc cho truy vấn cán bộ
     */
    private Specification<Staff> buildSpecification(StaffFilterDto filter, Long forcedStaffId,
            List<ResolvedScope> resolvedScopes) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new java.util.ArrayList<>();

            if (hasText(filter.getStaffCode())) {
                predicates
                        .add(cb.like(cb.lower(root.get("staffCode")), "%" + filter.getStaffCode().toLowerCase() + "%"));
            }
            if (hasText(filter.getFullName())) {
                predicates.add(cb.like(cb.lower(root.get("fullName")), "%" + filter.getFullName().toLowerCase() + "%"));
            }
            if (filter.getUnitId() != null) {
                predicates.add(cb.equal(root.get("unit").get("id"), filter.getUnitId()));
            }
            if (hasText(filter.getStatus())) {
                predicates.add(cb.equal(root.get("status"), filter.getStatus()));
            }
            if (hasText(filter.getGender())) {
                predicates.add(cb.equal(root.get("gender"), filter.getGender()));
            }
            if (hasText(filter.getPhone())) {
                predicates.add(cb.like(root.get("phone"), "%" + filter.getPhone() + "%"));
            }
            if (hasText(filter.getEmail())) {
                predicates.add(cb.like(cb.lower(root.get("email")), "%" + filter.getEmail().toLowerCase() + "%"));
            }
            if (forcedStaffId != null) {
                predicates.add(cb.equal(root.get("id"), forcedStaffId));
            }

            Predicate scopePredicate = buildStaffScopePredicate(cb, root, resolvedScopes, forcedStaffId);
            if (scopePredicate != null) {
                predicates.add(scopePredicate);
            }

            // Always filter deleted flag
            predicates.add(cb.equal(root.get("deletedFlag"), 0));

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private void validateStaffAccess(Staff staff, ActionType actionType) {
        Long currentStaffId = resolveCurrentStaffIdForSelfScope(actionType);
        List<ResolvedScope> resolvedScopes = ScopeFilterUtils.getScopesForQuery(FEATURE, actionType);
        if (!hasStaffAccess(staff, resolvedScopes, currentStaffId)) {
            throw new UserMessageException(CommonErrorCode.ACCESS_DENIED);
        }
    }

    private Predicate buildStaffScopePredicate(jakarta.persistence.criteria.CriteriaBuilder cb,
            jakarta.persistence.criteria.Root<Staff> root,
            List<ResolvedScope> resolvedScopes,
            Long currentStaffId) {
        if (resolvedScopes == null || resolvedScopes.isEmpty()) {
            return cb.disjunction();
        }

        List<Predicate> requiredPredicates = new java.util.ArrayList<>();
        List<Predicate> selfPredicates = new java.util.ArrayList<>();
        boolean hasUnitScope = false;
        boolean hasGradeScope = false;
        for (ResolvedScope scope : resolvedScopes) {
            if (scope == null) {
                continue;
            }
            if (scope.isUnrestricted() || scope.getScopeType() == ScopeType.ALL) {
                return cb.conjunction();
            }

            switch (scope.getScopeType()) {
                case SELF -> {
                    if (currentStaffId != null) {
                        selfPredicates.add(cb.equal(root.get("id"), currentStaffId));
                    }
                }
                case UNIT -> {
                    if (scope.getScopeIds() != null && !scope.getScopeIds().isEmpty()) {
                        hasUnitScope = true;
                        requiredPredicates.add(root.get("unit").get("id").in(scope.getScopeIds()));
                    }
                }
                case GRADE -> {
                    if (scope.getScopeIds() != null && !scope.getScopeIds().isEmpty()) {
                        hasGradeScope = true;
                        requiredPredicates.add(root.get("gradeLevel").get("id").in(scope.getScopeIds()));
                    }
                }
                default -> {
                }
            }
        }

        if (!selfPredicates.isEmpty()) {
            return cb.or(selfPredicates.toArray(new Predicate[0]));
        }

        if (hasUnitScope || hasGradeScope) {
            return requiredPredicates.isEmpty()
                    ? cb.disjunction()
                    : cb.and(requiredPredicates.toArray(new Predicate[0]));
        }

        return cb.disjunction();
    }

    private boolean hasStaffAccess(Staff staff, List<ResolvedScope> resolvedScopes, Long currentStaffId) {
        if (staff == null || resolvedScopes == null || resolvedScopes.isEmpty()) {
            return false;
        }

        for (ResolvedScope scope : resolvedScopes) {
            if (scope == null) {
                continue;
            }
            if (scope.isUnrestricted() || scope.getScopeType() == ScopeType.ALL) {
                return true;
            }
            switch (scope.getScopeType()) {
                case SELF -> {
                    if (currentStaffId != null && currentStaffId.equals(staff.getId())) {
                        return true;
                    }
                }
                case UNIT -> {
                    if (staff.getUnit() != null && scope.getScopeIds() != null
                            && scope.getScopeIds().contains(staff.getUnit().getId())) {
                        return true;
                    }
                }
                case GRADE -> {
                    if (staff.getGradeLevel() != null && scope.getScopeIds() != null
                            && scope.getScopeIds().contains(staff.getGradeLevel().getId())) {
                        return true;
                    }
                }
                default -> {
                }
            }
        }

        return false;
    }

    private Long resolveCurrentStaffIdForSelfScope(ActionType actionType) {
        List<ResolvedScope> scopes = ScopeFilterUtils.getScopesForQuery(FEATURE, actionType);
        boolean unrestricted = scopes.stream().anyMatch(ResolvedScope::isUnrestricted);
        if (unrestricted) {
            return null;
        }

        boolean hasSelfScope = scopes.stream().anyMatch(scope -> scope.getScopeType() == ScopeType.SELF);
        if (!hasSelfScope) {
            return null;
        }

        String username = SecurityContextUtils.getCurrentUsername();
        if (username == null) {
            throw new UserMessageException(CommonErrorCode.ACCESS_DENIED);
        }

        return userRepository.findByUsernameWithStaffAndRole(username)
                .map(user -> user.getStaff() != null ? user.getStaff().getId() : null)
                .orElseThrow(() -> new UserMessageException(CommonErrorCode.ACCESS_DENIED));
    }

    /*
     * Áp dụng các trường thông tin của cán bộ từ request vào đối tượng Staff
     */
    private void applyStaffFields(Staff staff, StaffCreateRequest request) {
        staff.setFullName(normalize(request.getFullName()));
        staff.setAliasName(normalizeNullable(request.getAliasName()));
        staff.setIdentityCode(normalizeNullable(request.getIdentityCode()));
        staff.setGender(request.getGender());
        staff.setDateOfBirth(request.getDateOfBirth());
        staff.setEthnicityId(request.getEthnicityId());
        staff.setReligionId(request.getReligionId());
        staff.setNationalityId(request.getNationalityId());
        staff.setCccdNo(normalizeNullable(request.getCccdNo()));
        staff.setCccdIssueDate(request.getCccdIssueDate());
        staff.setCccdIssuePlace(normalizeNullable(request.getCccdIssuePlace()));
        staff.setPhone(normalizeNullable(request.getPhone()));
        staff.setEmail(normalizeNullable(request.getEmail()));
        staff.setHealthStatus(normalizeNullable(request.getHealthStatus()));
        staff.setSocialInsuranceNo(normalizeNullable(request.getSocialInsuranceNo()));
        staff.setAvatarFileId(request.getAvatarFileId());
        staff.setAvatarUrl(normalizeNullable(request.getAvatarUrl()));
        staff.setSignatureFileId(request.getSignatureFileId());
        staff.setSignatureUrl(normalizeNullable(request.getSignatureUrl()));
        staff.setNote(normalizeNullable(request.getNote()));
    }

    /*
     * Áp dụng các trường thông tin của cán bộ từ request vào đối tượng Staff
     */
    private void applyStaffFields(Staff staff, StaffUpdateRequest request) {
        staff.setFullName(normalize(request.getFullName()));
        staff.setAliasName(normalizeNullable(request.getAliasName()));
        staff.setIdentityCode(normalizeNullable(request.getIdentityCode()));
        staff.setGender(request.getGender());
        staff.setDateOfBirth(request.getDateOfBirth());
        staff.setEthnicityId(request.getEthnicityId());
        staff.setReligionId(request.getReligionId());
        staff.setNationalityId(request.getNationalityId());
        staff.setCccdNo(normalizeNullable(request.getCccdNo()));
        staff.setCccdIssueDate(request.getCccdIssueDate());
        staff.setCccdIssuePlace(normalizeNullable(request.getCccdIssuePlace()));
        staff.setPhone(normalizeNullable(request.getPhone()));
        staff.setEmail(normalizeNullable(request.getEmail()));
        staff.setHealthStatus(normalizeNullable(request.getHealthStatus()));
        staff.setSocialInsuranceNo(normalizeNullable(request.getSocialInsuranceNo()));
        staff.setAvatarFileId(request.getAvatarFileId());
        staff.setAvatarUrl(normalizeNullable(request.getAvatarUrl()));
        staff.setSignatureFileId(request.getSignatureFileId());
        staff.setSignatureUrl(normalizeNullable(request.getSignatureUrl()));
        staff.setStatus(request.getStatus() != null ? request.getStatus() : staff.getStatus());
        staff.setNote(normalizeNullable(request.getNote()));
    }

    /*
     * Chuyển đổi đối tượng Staff thành DTO cho hiển thị danh sách
     */
    private StaffItemDto toItemDto(Staff staff) {
        return StaffItemDto.builder()
                .id(staff.getId())
                .staffCode(staff.getStaffCode())
                .fullName(staff.getFullName())
                .aliasName(staff.getAliasName())
                .unitId(staff.getUnit().getId())
                .unitName(staff.getUnit().getName())
                .gradeId(staff.getGradeLevel() == null ? null : staff.getGradeLevel().getId())
                .gender(staff.getGender())
                .dateOfBirth(staff.getDateOfBirth())
                .phone(staff.getPhone())
                .email(staff.getEmail())
                .status(staff.getStatus())
                .cccdNo(staff.getCccdNo())
                .avatarUrl(staff.getAvatarUrl())
                .build();
    }

    /*
     * Chuyển đổi đối tượng Staff thành DTO cho hiển thị danh sách chức vụ
     */
    private StaffGradeItemDto toGradeItemDto(Staff staff) {
        return StaffGradeItemDto.builder()
                .id(staff.getId())
                .staffCode(staff.getStaffCode())
                .fullName(staff.getFullName())
                .unitId(staff.getUnit() == null ? null : staff.getUnit().getId())
                .gradeId(staff.getGradeLevel() == null ? null : staff.getGradeLevel().getId())
                .build();
    }

    /*
     * Chuyển đổi đối tượng Staff thành DTO cho hiển thị chi tiết
     * Bao gồm thông tin cơ bản và chi tiết như địa chỉ, thành viên gia đình
     */
    private StaffDetailDto toDetailDto(Staff staff) {
        List<StaffAddress> addresses = staffAddressRepository.findByStaffId(staff.getId());
        List<StaffFamilyMember> familyMembers = staffFamilyMemberRepository.findByStaffId(staff.getId());
        return toDetailDto(staff, addresses, familyMembers);
    }

    /*
     * Chuyển đổi đối tượng Staff thành DTO cho hiển thị chi tiết
     * Bao gồm thông tin cơ bản và chi tiết như địa chỉ, thành viên gia đình
     * Sử dụng map để ánh xạ địa chỉ và thành viên gia đình theo loại để dễ dàng
     * truy xuất khi xây dựng DTO
     */
    private StaffDetailDto toDetailDto(Staff staff, List<StaffAddress> addresses,
            List<StaffFamilyMember> familyMembers) {
        Map<String, StaffAddress> addressMap = safeList(addresses).stream()
                .collect(Collectors.toMap(StaffAddress::getAddressType, Function.identity(), (first, second) -> first));
        Map<String, StaffFamilyMember> familyMap = safeList(familyMembers).stream()
                .collect(Collectors.toMap(StaffFamilyMember::getRelationType, Function.identity(),
                        (first, second) -> first));
        return StaffDetailDto.builder()
                .id(staff.getId())
                .userId(staff.getUser() != null ? staff.getUser().getId() : null)
                .unitId(staff.getUnit().getId())
                .gradeId(staff.getGradeLevel() == null ? null : staff.getGradeLevel().getId())
                .staffCode(staff.getStaffCode())
                .identityCode(staff.getIdentityCode())
                .fullName(staff.getFullName())
                .aliasName(staff.getAliasName())
                .gender(staff.getGender())
                .dateOfBirth(staff.getDateOfBirth())
                .ethnicityId(staff.getEthnicityId())
                .religionId(staff.getReligionId())
                .nationalityId(staff.getNationalityId())
                .cccdNo(staff.getCccdNo())
                .cccdIssueDate(staff.getCccdIssueDate())
                .cccdIssuePlace(staff.getCccdIssuePlace())
                .phone(staff.getPhone())
                .email(staff.getEmail())
                .healthStatus(staff.getHealthStatus())
                .socialInsuranceNo(staff.getSocialInsuranceNo())
                .avatarFileId(staff.getAvatarFileId())
                .avatarUrl(staff.getAvatarUrl())
                .signatureFileId(staff.getSignatureFileId())
                .signatureUrl(staff.getSignatureUrl())
                .status(staff.getStatus())
                .note(staff.getNote())
                .permanentAddress(toAddressDto(addressMap.get(ADDRESS_TYPE_PERMANENT)))
                .temporaryAddress(toAddressDto(addressMap.get(ADDRESS_TYPE_TEMPORARY)))
                .birthPlaceAddress(toAddressDto(addressMap.get(ADDRESS_TYPE_BIRTH_PLACE)))
                .fatherInfo(toFamilyDto(familyMap.get(RELATION_FATHER)))
                .motherInfo(toFamilyDto(familyMap.get(RELATION_MOTHER)))
                .spouseInfo(toFamilyDto(familyMap.get(RELATION_SPOUSE)))
                .spouseFatherInfo(toFamilyDto(familyMap.get(RELATION_SPOUSE_FATHER)))
                .spouseMotherInfo(toFamilyDto(familyMap.get(RELATION_SPOUSE_MOTHER)))
                .childrenDetail(extractChildrenDetail(familyMap.get(RELATION_CHILDREN)))
                .build();
    }

    /*
     * Thay thế thông tin địa chỉ của cán bộ
     * Xóa tất cả địa chỉ cũ của cán bộ và tạo mới theo thông tin từ request
     * Đảm bảo thực hiện xóa trước khi thêm mới để tránh lỗi trùng khóa nếu có
     * unique constraint trên bảng địa chỉ
     */
    private List<StaffAddress> replaceAddresses(Staff staff, StaffCreateRequest request) {
        return replaceAddresses(staff, request.getPermanentAddress(), request.getTemporaryAddress(),
                request.getBirthPlaceAddress());
    }

    /*
     * Thay thế thông tin địa chỉ của cán bộ
     * Xóa tất cả địa chỉ cũ của cán bộ và tạo mới theo thông tin từ request
     * Đảm bảo thực hiện xóa trước khi thêm mới để tránh lỗi trùng khóa nếu có
     * unique constraint trên bảng địa chỉ
     */
    private List<StaffAddress> replaceAddresses(Staff staff, StaffUpdateRequest request) {
        return replaceAddresses(staff, request.getPermanentAddress(), request.getTemporaryAddress(),
                request.getBirthPlaceAddress());
    }

    /*
     * Thay thế thông tin địa chỉ của cán bộ
     * Xóa tất cả địa chỉ cũ của cán bộ và tạo mới theo thông tin từ request
     * Đảm bảo thực hiện xóa trước khi thêm mới để tránh lỗi trùng khóa nếu có
     * unique constraint trên bảng địa chỉ
     */
    private List<StaffAddress> replaceAddresses(Staff staff, StaffAddressRequest permanentAddress,
            StaffAddressRequest temporaryAddress, StaffAddressRequest birthPlaceAddress) {
        staffAddressRepository.deleteByStaffId(staff.getId());
        // Ensure delete is flushed to DB before inserting new addresses to avoid unique
        // constraint errors
        staffAddressRepository.flush();
        List<StaffAddress> addresses = Stream.of(
                buildAddress(staff, ADDRESS_TYPE_PERMANENT, permanentAddress),
                buildAddress(staff, ADDRESS_TYPE_TEMPORARY, temporaryAddress),
                buildAddress(staff, ADDRESS_TYPE_BIRTH_PLACE, birthPlaceAddress))
                .filter(item -> item != null)
                .toList();
        if (addresses.isEmpty()) {
            return List.of();
        }
        // debug log to help diagnose duplicates
        log.debug("Saving staff addresses for staffId={} count={}", staff.getId(), addresses.size());
        return staffAddressRepository.saveAll(addresses);
    }

    /*
     * Xây dựng đối tượng địa chỉ cho cán bộ dựa trên loại địa chỉ và thông tin từ
     * request
     */
    private StaffAddress buildAddress(Staff staff, String addressType, StaffAddressRequest request) {
        if (request == null || isAddressEmpty(request)) {
            return null;
        }
        StaffAddress address = new StaffAddress();
        address.setStaff(staff);
        address.setAddressType(addressType);
        address.setProvinceId(request.getProvinceId());
        address.setDistrictId(request.getDistrictId());
        address.setWardId(request.getWardId());
        address.setHamletName(normalizeNullable(request.getHamletName()));
        address.setDetailAddress(normalizeNullable(request.getDetailAddress()));
        address.setFullAddress(normalizeNullable(request.getFullAddress()));
        return address;
    }

    /*
     * Thay thế thông tin thành viên gia đình của cán bộ
     * Xóa tất cả thành viên gia đình cũ của cán bộ và tạo mới theo thông tin từ
     * request
     * Đảm bảo thực hiện xóa trước khi thêm mới để tránh lỗi trùng khóa nếu có
     * unique constraint trên bảng thành viên gia đình
     */
    private List<StaffFamilyMember> replaceFamilyMembers(Staff staff, StaffCreateRequest request) {
        return replaceFamilyMembers(staff, request.getFatherInfo(), request.getMotherInfo(), request.getSpouseInfo(),
                request.getSpouseFatherInfo(), request.getSpouseMotherInfo(), request.getChildrenDetail());
    }

    /*
     * Thay thế thông tin thành viên gia đình của cán bộ
     * Xóa tất cả thành viên gia đình cũ của cán bộ và tạo mới theo thông tin từ
     * request
     * Đảm bảo thực hiện xóa trước khi thêm mới để tránh lỗi trùng khóa nếu có
     * unique constraint trên bảng thành viên gia đình
     */
    private List<StaffFamilyMember> replaceFamilyMembers(Staff staff, StaffUpdateRequest request) {
        return replaceFamilyMembers(staff, request.getFatherInfo(), request.getMotherInfo(), request.getSpouseInfo(),
                request.getSpouseFatherInfo(), request.getSpouseMotherInfo(), request.getChildrenDetail());
    }

    /*
     * Thay thế thông tin thành viên gia đình của cán bộ
     * Xóa tất cả thành viên gia đình cũ của cán bộ và tạo mới theo thông tin từ
     * request
     * Đảm bảo thực hiện xóa trước khi thêm mới để tránh lỗi trùng khóa nếu có
     * unique constraint trên bảng thành viên gia đình
     */
    private List<StaffFamilyMember> replaceFamilyMembers(Staff staff, StaffFamilyMemberRequest fatherInfo,
            StaffFamilyMemberRequest motherInfo, StaffFamilyMemberRequest spouseInfo,
            StaffFamilyMemberRequest spouseFatherInfo, StaffFamilyMemberRequest spouseMotherInfo,
            String childrenDetail) {
        staffFamilyMemberRepository.deleteByStaffId(staff.getId());
        List<StaffFamilyMember> familyMembers = Stream.of(
                buildFamilyMember(staff, RELATION_FATHER, fatherInfo),
                buildFamilyMember(staff, RELATION_MOTHER, motherInfo),
                buildFamilyMember(staff, RELATION_SPOUSE, spouseInfo),
                buildFamilyMember(staff, RELATION_SPOUSE_FATHER, spouseFatherInfo),
                buildFamilyMember(staff, RELATION_SPOUSE_MOTHER, spouseMotherInfo),
                buildChildrenMember(staff, childrenDetail))
                .filter(item -> item != null)
                .toList();
        return familyMembers.isEmpty() ? List.of() : staffFamilyMemberRepository.saveAll(familyMembers);
    }

    /*
     * Xây dựng đối tượng thành viên gia đình cho cán bộ dựa trên loại quan hệ và
     * thông tin từ request
     */
    private StaffFamilyMember buildFamilyMember(Staff staff, String relationType, StaffFamilyMemberRequest request) {
        if (request == null || isFamilyMemberEmpty(request)) {
            return null;
        }
        StaffFamilyMember member = new StaffFamilyMember();
        member.setStaff(staff);
        member.setRelationType(relationType);
        String fullName = normalizeNullable(request.getFullName());
        member.setFullName(fullName != null ? fullName : relationType);
        member.setBirthYear(request.getBirthYear());
        member.setPlaceOfBirth(normalizeNullable(request.getPlaceOfBirth()));
        member.setHometown(normalizeNullable(request.getHometown()));
        member.setOccupation(normalizeNullable(request.getOccupation()));
        member.setPhone(normalizeNullable(request.getPhone()));
        member.setWorkplace(normalizeNullable(request.getWorkplace()));
        member.setAddress(normalizeNullable(request.getAddress()));
        member.setNote(normalizeNullable(request.getNote()));
        return member;
    }

    /*
     * Xây dựng đối tượng thành viên gia đình cho cán bộ với loại quan hệ là con cái
     * dựa trên thông tin chi tiết từ request
     * Do con cái có thể có nhiều người nên không có trường fullName cụ thể, sẽ lưu
     * thông tin chi tiết vào trường note để hiển thị
     */
    private StaffFamilyMember buildChildrenMember(Staff staff, String childrenDetail) {
        String normalizedDetail = normalizeNullable(childrenDetail);
        if (normalizedDetail == null) {
            return null;
        }
        StaffFamilyMember member = new StaffFamilyMember();
        member.setStaff(staff);
        member.setRelationType(RELATION_CHILDREN);
        member.setFullName("CON");
        member.setNote(normalizedDetail);
        return member;
    }

    /*
     * Chuyển đổi đối tượng địa chỉ của cán bộ sang DTO
     */
    private StaffAddressDto toAddressDto(StaffAddress address) {
        if (address == null) {
            return null;
        }
        return StaffAddressDto.builder()
                .id(address.getId())
                .addressType(address.getAddressType())
                .provinceId(address.getProvinceId())
                .districtId(address.getDistrictId())
                .wardId(address.getWardId())
                .hamletName(address.getHamletName())
                .detailAddress(address.getDetailAddress())
                .fullAddress(address.getFullAddress())
                .build();
    }

    /*
     * Chuyển đổi đối tượng thành viên gia đình của cán bộ sang DTO
     */
    private StaffFamilyMemberDto toFamilyDto(StaffFamilyMember familyMember) {
        if (familyMember == null) {
            return null;
        }
        return StaffFamilyMemberDto.builder()
                .id(familyMember.getId())
                .relationType(familyMember.getRelationType())
                .fullName(familyMember.getFullName())
                .birthYear(familyMember.getBirthYear())
                .placeOfBirth(familyMember.getPlaceOfBirth())
                .hometown(familyMember.getHometown())
                .occupation(familyMember.getOccupation())
                .phone(familyMember.getPhone())
                .workplace(familyMember.getWorkplace())
                .address(familyMember.getAddress())
                .note(familyMember.getNote())
                .build();
    }

    /*
     * Trích xuất thông tin chi tiết con cái từ trường note của thành viên gia đình
     * có loại quan hệ là con cái
     * Do con cái có thể có nhiều người nên không có trường fullName cụ thể, sẽ lưu
     * thông tin chi tiết vào trường note để hiển thị
     */
    private String extractChildrenDetail(StaffFamilyMember childrenMember) {
        return childrenMember == null ? null : childrenMember.getNote();
    }

    /*
     * Áp dụng các trường thông tin đa phương tiện cho cán bộ
     */
    private void applyMediaFields(Staff staff, Unit unit, String avatarUrl, String signatureUrl) {
        if (unit == null) {
            return;
        }
        String yearLabel = String.valueOf(LocalDate.now().getYear());
        if (avatarUrl != null && !avatarUrl.isBlank()) {
            staff.setAvatarUrl(fileStorageService.storeStaffAvatarFromDataUrl(avatarUrl, unit.getName(), yearLabel));
        }
        if (signatureUrl != null && !signatureUrl.isBlank()) {
            staff.setSignatureUrl(
                    fileStorageService.storeStaffSignatureFromDataUrl(signatureUrl, unit.getName(), yearLabel));
        }
    }

    /*
     * Tìm kiếm cán bộ theo ID, nếu không tìm thấy sẽ ném lỗi UserMessageException
     * với mã lỗi STAFF_NOT_FOUND
     */
    private Staff findStaff(Long id) {
        return staffRepository.findById(id)
                .orElseThrow(() -> new UserMessageException(CommonErrorCode.STAFF_NOT_FOUND));
    }

    /*
     * Kiểm tra xem một chuỗi có chứa văn bản hợp lệ không
     */
    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    /*
     * Chuẩn hóa chuỗi bằng cách loại bỏ khoảng trắng ở đầu và cuối, nếu chuỗi null
     * sẽ trả về null
     */
    private String normalize(String value) {
        return value == null ? null : value.trim();
    }

    /*
     * Chuẩn hóa chuỗi có thể null, nếu chuỗi null hoặc chỉ chứa khoảng trắng sẽ trả
     * về null
     */
    private String normalizeNullable(String value) {
        return hasText(value) ? value.trim() : null;
    }

    /*
     * Kiểm tra xem địa chỉ của cán bộ có trống không
     */
    private boolean isAddressEmpty(StaffAddressRequest request) {
        return request.getProvinceId() == null
                && request.getDistrictId() == null
                && request.getWardId() == null
                && !hasText(request.getHamletName())
                && !hasText(request.getDetailAddress())
                && !hasText(request.getFullAddress());
    }

    /*
     * Kiểm tra xem thông tin thành viên gia đình có trống không
     */
    private boolean isFamilyMemberEmpty(StaffFamilyMemberRequest request) {
        return !hasText(request.getFullName())
                && request.getBirthYear() == null
                && !hasText(request.getPlaceOfBirth())
                && !hasText(request.getHometown())
                && !hasText(request.getOccupation())
                && !hasText(request.getPhone())
                && !hasText(request.getWorkplace())
                && !hasText(request.getAddress())
                && !hasText(request.getNote());
    }

    /*
     * Tạo danh sách an toàn, nếu danh sách đầu vào là null thì trả về danh sách
     * rỗng
     */
    private <T> List<T> safeList(List<T> items) {
        return items == null ? List.of() : items;
    }

    /*
     * Chuẩn hóa kích thước trang, nếu giá trị null hoặc không hợp lệ thì trả về 20
     */
    private int normalizePageSize(Integer pageSize) {
        return pageSize == null || pageSize <= 0 ? 20 : pageSize;
    }

    /*
     * Chuẩn hóa số trang hiện tại, nếu giá trị null hoặc không hợp lệ thì trả về 0
     */
    private int normalizePageNow(Integer pageNow) {
        return pageNow == null || pageNow < 0 ? 0 : pageNow;
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] export(PageRequestDto<StaffFilterDto> request, Long unitId, ExportType exportType) {
        StaffFilterDto filter = request.getFilter() == null ? new StaffFilterDto() : request.getFilter();
        if (unitId != null) {
            filter.setUnitId(unitId);
        }
        PageRequestDto<StaffFilterDto> exportRequest = new PageRequestDto<>();
        exportRequest.setFilter(filter);
        exportRequest.setPageNow(1);
        exportRequest.setPageSize(Integer.MAX_VALUE);
        List<StaffItemDto> items = search(exportRequest).getItems();
        if (exportType == ExportType.PDF) {
            return buildStaffExportPdf(items, unitId);
        }
        return buildStaffExportWorkbook(items, unitId);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportExcelTemplate(Long unitId) {
        Unit unit = unitRepository.findById(unitId)
                .orElseThrow(() -> new UserMessageException(CommonErrorCode.UNIT_NOT_FOUND));
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("CanBo");
            CellStyle govHeaderStyle = createExcelGovHeaderStyle(workbook);
            CellStyle govSubHeaderStyle = createExcelGovSubHeaderStyle(workbook);
            CellStyle titleStyle = createExcelTitleStyle(workbook);
            CellStyle headerStyle = createExcelHeaderStyle(workbook);
            CellStyle guideStyle = createExcelGuideStyle(workbook);
            CellStyle bodyStyle = createExcelBodyStyle(workbook);
            int middleColumn = STAFF_IMPORT_LAST_DATA_COLUMN / 2;

            Row govHeaderRow = sheet.createRow(0);
            createCell(govHeaderRow, 0, "BỘ GIÁO DỤC VÀ ĐÀO TẠO", govHeaderStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, middleColumn));
            createCell(govHeaderRow, middleColumn + 1, "CỘNG HÒA XÃ HỘI CHỦ NGHĨA VIỆT NAM", govHeaderStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, middleColumn + 1, STAFF_IMPORT_LAST_DATA_COLUMN));

            Row govSubHeaderRow = sheet.createRow(1);
            createCell(govSubHeaderRow, 0, unit.getName(), govSubHeaderStyle);
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, middleColumn));
            createCell(govSubHeaderRow, middleColumn + 1, "Độc lập - Tự do - Hạnh phúc", govSubHeaderStyle);
            sheet.addMergedRegion(new CellRangeAddress(1, 1, middleColumn + 1, STAFF_IMPORT_LAST_DATA_COLUMN));

            createCell(sheet.createRow(3), 0, "MẪU IMPORT CÁN BỘ", titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(3, 3, 0, STAFF_IMPORT_LAST_DATA_COLUMN));
            createCell(sheet.createRow(4), 0,
                    "Cột bắt buộc: Họ tên. Mã cán bộ sẽ được tự sinh theo đơn vị khi import.",
                    guideStyle);
            sheet.addMergedRegion(new CellRangeAddress(4, 4, 0, STAFF_IMPORT_LAST_DATA_COLUMN));
            createCell(sheet.createRow(5), 0,
                    "Giới tính: Nam/Nữ/Khác. Trạng thái: Đang làm việc/Ngừng hoạt động. Dân tộc chỉ được nhập theo sheet DanToc. Các cột còn lại là tùy chọn.",
                    guideStyle);
            sheet.addMergedRegion(new CellRangeAddress(5, 5, 0, STAFF_IMPORT_LAST_DATA_COLUMN));

            Row groupHeaderRow = sheet.createRow(STAFF_TEMPLATE_GROUP_HEADER_ROW_INDEX);
            fillRowWithStyle(groupHeaderRow, 0, STAFF_IMPORT_LAST_DATA_COLUMN, headerStyle);
            createCell(groupHeaderRow, 0, "THÔNG TIN CÁN BỘ", headerStyle);
            sheet.addMergedRegion(new CellRangeAddress(STAFF_TEMPLATE_GROUP_HEADER_ROW_INDEX,
                    STAFF_TEMPLATE_GROUP_HEADER_ROW_INDEX, 0, STAFF_COL_BIRTH_PLACE_ADDRESS));
            createCell(groupHeaderRow, STAFF_COL_FATHER_NAME, "Cha", headerStyle);
            sheet.addMergedRegion(new CellRangeAddress(STAFF_TEMPLATE_GROUP_HEADER_ROW_INDEX,
                    STAFF_TEMPLATE_GROUP_HEADER_ROW_INDEX, STAFF_COL_FATHER_NAME, STAFF_COL_FATHER_PHONE));
            createCell(groupHeaderRow, STAFF_COL_MOTHER_NAME, "Mẹ", headerStyle);
            sheet.addMergedRegion(new CellRangeAddress(STAFF_TEMPLATE_GROUP_HEADER_ROW_INDEX,
                    STAFF_TEMPLATE_GROUP_HEADER_ROW_INDEX, STAFF_COL_MOTHER_NAME, STAFF_COL_MOTHER_PHONE));
            createCell(groupHeaderRow, STAFF_COL_SPOUSE_NAME, "Vợ/Chồng", headerStyle);
            sheet.addMergedRegion(new CellRangeAddress(STAFF_TEMPLATE_GROUP_HEADER_ROW_INDEX,
                    STAFF_TEMPLATE_GROUP_HEADER_ROW_INDEX, STAFF_COL_SPOUSE_NAME, STAFF_COL_SPOUSE_PHONE));
            createCell(groupHeaderRow, STAFF_COL_SPOUSE_FATHER_NAME, "Bố vợ/chồng", headerStyle);
            sheet.addMergedRegion(new CellRangeAddress(STAFF_TEMPLATE_GROUP_HEADER_ROW_INDEX,
                    STAFF_TEMPLATE_GROUP_HEADER_ROW_INDEX, STAFF_COL_SPOUSE_FATHER_NAME,
                    STAFF_COL_SPOUSE_FATHER_PHONE));
            createCell(groupHeaderRow, STAFF_COL_SPOUSE_MOTHER_NAME, "Mẹ vợ/chồng", headerStyle);
            sheet.addMergedRegion(new CellRangeAddress(STAFF_TEMPLATE_GROUP_HEADER_ROW_INDEX,
                    STAFF_TEMPLATE_GROUP_HEADER_ROW_INDEX, STAFF_COL_SPOUSE_MOTHER_NAME,
                    STAFF_COL_SPOUSE_MOTHER_PHONE));
            createCell(groupHeaderRow, STAFF_COL_CHILDREN_DETAIL, "Con", headerStyle);
            createCell(groupHeaderRow, STAFF_COL_TRAINING_SCHOOL, "Đào tạo", headerStyle);
            sheet.addMergedRegion(new CellRangeAddress(STAFF_TEMPLATE_GROUP_HEADER_ROW_INDEX,
                    STAFF_TEMPLATE_GROUP_HEADER_ROW_INDEX, STAFF_COL_TRAINING_SCHOOL, STAFF_COL_TRAINING_NOTE));
            createCell(groupHeaderRow, STAFF_COL_FOREIGN_LANGUAGE_NAME, "Ngoại ngữ", headerStyle);
            sheet.addMergedRegion(new CellRangeAddress(STAFF_TEMPLATE_GROUP_HEADER_ROW_INDEX,
                    STAFF_TEMPLATE_GROUP_HEADER_ROW_INDEX, STAFF_COL_FOREIGN_LANGUAGE_NAME,
                    STAFF_COL_FOREIGN_LANGUAGE_NOTE));

            Row headerRow = sheet.createRow(STAFF_IMPORT_HEADER_ROW_INDEX);
            String[] headers = {
                    "STT", "Họ tên *", "Tên gọi khác", "Giới tính", "Ngày sinh",
                    "Điện thoại", "Email", "Khối", "Trạng thái", "Ghi chú", "Mã định danh",
                    "Dân tộc", "Tôn giáo", "Quốc tịch", "CCCD/CMND", "Ngày cấp CCCD", "Nơi cấp CCCD",
                    "Tình trạng sức khỏe", "Số BHXH", "Địa chỉ thường trú", "Địa chỉ tạm trú", "Địa chỉ nơi sinh",
                    "Họ tên", "Năm sinh", "Quê quán", "Nghề nghiệp", "Điện thoại",
                    "Họ tên", "Năm sinh", "Quê quán", "Nghề nghiệp", "Điện thoại",
                    "Họ tên", "Năm sinh", "Nghề nghiệp", "Điện thoại",
                    "Họ tên", "Năm sinh", "Quê quán",
                    "Nghề nghiệp", "Điện thoại",
                    "Họ tên", "Năm sinh", "Quê quán",
                    "Nghề nghiệp", "Điện thoại", "Thông tin con"
            };
            headers = java.util.Arrays.copyOf(headers, STAFF_IMPORT_LAST_DATA_COLUMN + 1);
            headers[STAFF_COL_TRAINING_SCHOOL] = "Trường đào tạo";
            headers[STAFF_COL_TRAINING_MAJOR] = "Chuyên ngành";
            headers[STAFF_COL_TRAINING_FORM] = "Hình thức đào tạo";
            headers[STAFF_COL_TRAINING_CERTIFICATE] = "Chứng chỉ";
            headers[STAFF_COL_TRAINING_FROM_DATE] = "Từ ngày";
            headers[STAFF_COL_TRAINING_TO_DATE] = "Đến ngày";
            headers[STAFF_COL_TRAINING_NOTE] = "Ghi chú đào tạo";
            headers[STAFF_COL_FOREIGN_LANGUAGE_NAME] = "Tên ngoại ngữ";
            headers[STAFF_COL_FOREIGN_LANGUAGE_LEVEL] = "Trình độ";
            headers[STAFF_COL_FOREIGN_LANGUAGE_ISSUE_DATE] = "Ngày cấp";
            headers[STAFF_COL_FOREIGN_LANGUAGE_SCORE] = "Điểm/kết quả";
            headers[STAFF_COL_FOREIGN_LANGUAGE_NOTE] = "Ghi chú ngoại ngữ";
            for (int i = 0; i < headers.length; i++) {
                createCell(headerRow, i, headers[i], headerStyle);
            }

            Row sampleRow = sheet.createRow(STAFF_TEMPLATE_SAMPLE_ROW_INDEX);
            Object[] sampleValues = {
                    1, "Trần Thị B", "Thị B", "Nữ", "01/01/1990", "0912345678",
                    "b@example.com", "", "Đang làm việc", "Dữ liệu mẫu", "ID-CB-001",
                    "Kinh", "Không", "Việt Nam", "079090012345", "10/03/2018", "Cục CSQLHC",
                    "Tốt", "BHXH001", "Số 1 Đường A", "Số 2 Đường B", "Buôn C",
                    "Trần Văn C", 1960, "Đắk Lắk", "Nông dân", "0900000001",
                    "Nguyễn Thị D", 1965, "Đắk Lắk", "Nội trợ", "0900000002",
                    "Lê Văn E", 1988, "Giáo viên", "0900000003",
                    "Lê Văn F", 1962, "Đắk Lắk", "Làm vườn", "0900000004",
                    "Phạm Thị G", 1964, "Đắk Lắk", "Nội trợ", "0900000005",
                    "Con trai: Nguyễn Văn A; Con gái: Nguyễn Thị B"
            };
            sampleValues = java.util.Arrays.copyOf(sampleValues, STAFF_IMPORT_LAST_DATA_COLUMN + 1);
            sampleValues[STAFF_COL_TRAINING_SCHOOL] = "DHSP Hà Nội";
            sampleValues[STAFF_COL_TRAINING_MAJOR] = "Sư phạm Toán học";
            sampleValues[STAFF_COL_TRAINING_FORM] = "Chính quy";
            sampleValues[STAFF_COL_TRAINING_CERTIFICATE] = "Cử nhân";
            sampleValues[STAFF_COL_TRAINING_FROM_DATE] = "01/09/2008";
            sampleValues[STAFF_COL_TRAINING_TO_DATE] = "30/06/2012";
            sampleValues[STAFF_COL_TRAINING_NOTE] = "Tốt nghiệp loại Giỏi";
            sampleValues[STAFF_COL_FOREIGN_LANGUAGE_NAME] = "Tiếng Anh";
            sampleValues[STAFF_COL_FOREIGN_LANGUAGE_LEVEL] = "B2";
            sampleValues[STAFF_COL_FOREIGN_LANGUAGE_ISSUE_DATE] = "10/08/2020";
            sampleValues[STAFF_COL_FOREIGN_LANGUAGE_SCORE] = "7.5";
            sampleValues[STAFF_COL_FOREIGN_LANGUAGE_NOTE] = "Chứng chỉ sư phạm tiếng Anh";
            for (int i = 0; i < sampleValues.length; i++) {
                createCell(sampleRow, i, sampleValues[i], bodyStyle);
            }

            createStaffEthnicitySheet(workbook);

            sheet.createFreezePane(0, STAFF_TEMPLATE_SAMPLE_ROW_INDEX);
            autosize(sheet, headers.length);
            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException ex) {
            throw new UserMessageException("Không thể tạo file Excel mẫu import cán bộ");
        }
    }

    @Override
    @Transactional
    public StaffImportResultDto importExcel(Long unitId, MultipartFile file) {
        Unit unit = unitRepository.findById(unitId)
                .orElseThrow(() -> new UserMessageException(CommonErrorCode.UNIT_NOT_FOUND));
        validateExcelFile(file);
        int successCount = 0;
        Map<Integer, String> rowErrors = new java.util.LinkedHashMap<>();

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();
            int headerRowIndex = findStaffImportHeaderRow(sheet, formatter);
            boolean hasLegacyCodeColumn = isLegacyStaffCodeColumn(sheet.getRow(headerRowIndex), formatter);
            int dataStartRowIndex = headerRowIndex + 1;

            for (int rowIndex = dataStartRowIndex; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                int fromColumn = hasLegacyCodeColumn ? STAFF_LEGACY_CODE_COLUMN_INDEX : STAFF_COL_FULL_NAME;
                int toColumn = hasLegacyCodeColumn ? STAFF_IMPORT_LAST_DATA_COLUMN + 1 : STAFF_IMPORT_LAST_DATA_COLUMN;
                if (isExcelRowEmpty(row, fromColumn, toColumn, formatter)) {
                    continue;
                }
                try {
                    upsertStaffFromExcelRow(unit, row, formatter, hasLegacyCodeColumn);
                    successCount++;
                } catch (Exception ex) {
                    rowErrors.put(rowIndex, ex.getMessage());
                }
            }
            String token = null;
            String fileName = null;
            if (!rowErrors.isEmpty()) {
                byte[] errorFile = buildStaffImportErrorFile(workbook, sheet, rowErrors, headerRowIndex);
                fileName = "staff-import-errors.xlsx";
                token = importErrorFileStorageService.store(fileName, EXCEL_CONTENT_TYPE, errorFile);
            }
            return StaffImportResultDto.builder()
                    .successCount(successCount)
                    .failedCount(rowErrors.size())
                    .hasErrorFile(token != null)
                    .errorFileToken(token)
                    .errorFileName(fileName)
                    .build();
        } catch (IOException ex) {
            throw new UserMessageException("Không thể đọc file Excel cán bộ");
        }
    }

    @Override
    public TemporaryFileDto getImportErrorFile(String token) {
        return importErrorFileStorageService.get(token);
    }

    private byte[] buildStaffExportWorkbook(List<StaffItemDto> items, Long unitId) {
        Unit unit = unitId == null ? null : unitRepository.findById(unitId).orElse(null);
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("CanBo");
            CellStyle infoStyle = createExcelInfoStyle(workbook);
            CellStyle govHeaderStyle = createExcelGovHeaderStyle(workbook);
            CellStyle govSubHeaderStyle = createExcelGovSubHeaderStyle(workbook);
            CellStyle titleStyle = createExcelTitleStyle(workbook);
            CellStyle headerStyle = createExcelHeaderStyle(workbook);
            CellStyle bodyStyle = createExcelBodyStyle(workbook);
            int lastColumn = STAFF_IMPORT_LAST_DATA_COLUMN;
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

            createCell(sheet.createRow(4), 0, "DANH SÁCH CÁN BỘ", titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(4, 4, 0, lastColumn));

            Row groupHeaderRow = sheet.createRow(6);
            fillRowWithStyle(groupHeaderRow, 0, lastColumn, headerStyle);
            createCell(groupHeaderRow, 0, "THÔNG TIN CÁN BỘ", headerStyle);
            sheet.addMergedRegion(new CellRangeAddress(6, 6, 0, STAFF_COL_BIRTH_PLACE_ADDRESS));
            createCell(groupHeaderRow, STAFF_COL_FATHER_NAME, "Cha", headerStyle);
            sheet.addMergedRegion(new CellRangeAddress(6, 6, STAFF_COL_FATHER_NAME, STAFF_COL_FATHER_PHONE));
            createCell(groupHeaderRow, STAFF_COL_MOTHER_NAME, "Mẹ", headerStyle);
            sheet.addMergedRegion(new CellRangeAddress(6, 6, STAFF_COL_MOTHER_NAME, STAFF_COL_MOTHER_PHONE));
            createCell(groupHeaderRow, STAFF_COL_SPOUSE_NAME, "Vợ/Chồng", headerStyle);
            sheet.addMergedRegion(new CellRangeAddress(6, 6, STAFF_COL_SPOUSE_NAME, STAFF_COL_SPOUSE_PHONE));
            createCell(groupHeaderRow, STAFF_COL_SPOUSE_FATHER_NAME, "Bố vợ/chồng", headerStyle);
            sheet.addMergedRegion(new CellRangeAddress(6, 6, STAFF_COL_SPOUSE_FATHER_NAME, STAFF_COL_SPOUSE_FATHER_PHONE));
            createCell(groupHeaderRow, STAFF_COL_SPOUSE_MOTHER_NAME, "Mẹ vợ/chồng", headerStyle);
            sheet.addMergedRegion(new CellRangeAddress(6, 6, STAFF_COL_SPOUSE_MOTHER_NAME, STAFF_COL_SPOUSE_MOTHER_PHONE));
            createCell(groupHeaderRow, STAFF_COL_CHILDREN_DETAIL, "Con", headerStyle);
            createCell(groupHeaderRow, STAFF_COL_TRAINING_SCHOOL, "Đào tạo", headerStyle);
            sheet.addMergedRegion(new CellRangeAddress(6, 6, STAFF_COL_TRAINING_SCHOOL, STAFF_COL_TRAINING_NOTE));
            createCell(groupHeaderRow, STAFF_COL_FOREIGN_LANGUAGE_NAME, "Ngoại ngữ", headerStyle);
            sheet.addMergedRegion(new CellRangeAddress(6, 6, STAFF_COL_FOREIGN_LANGUAGE_NAME, STAFF_COL_FOREIGN_LANGUAGE_NOTE));

            Row headerRow = sheet.createRow(7);
            String[] headers = buildStaffExportHeaders();
            for (int i = 0; i < headers.length; i++) {
                createCell(headerRow, i, headers[i], headerStyle);
            }
            int rowIndex = 8;
            int stt = 1;
            for (StaffItemDto item : items) {
                StaffDetailDto detail = getById(item.getId());
                Row row = sheet.createRow(rowIndex++);
                Object[] values = buildStaffExportRow(item, detail, stt++);
                for (int i = 0; i < values.length; i++) {
                    createCell(row, i, values[i], bodyStyle);
                }
            }
            autosize(sheet, headers.length);
            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException ex) {
            throw new UserMessageException("Không thể xuất cán bộ");
        }
    }

    private String[] buildStaffExportHeaders() {
        String[] headers = {
                "STT", "Họ tên", "Tên gọi khác", "Giới tính", "Ngày sinh",
                "Điện thoại", "Email", "Khối", "Trạng thái", "Ghi chú", "Mã định danh",
                "Dân tộc", "Tôn giáo", "Quốc tịch", "CCCD/CMND", "Ngày cấp CCCD", "Nơi cấp CCCD",
                "Tình trạng sức khỏe", "Số BHXH", "Địa chỉ thường trú", "Địa chỉ tạm trú", "Địa chỉ nơi sinh",
                "Cha - Họ tên", "Cha - Năm sinh", "Cha - Quê quán", "Cha - Nghề nghiệp", "Cha - Điện thoại",
                "Mẹ - Họ tên", "Mẹ - Năm sinh", "Mẹ - Quê quán", "Mẹ - Nghề nghiệp", "Mẹ - Điện thoại",
                "Vợ/Chồng - Họ tên", "Vợ/Chồng - Năm sinh", "Vợ/Chồng - Nghề nghiệp", "Vợ/Chồng - Điện thoại",
                "Bố vợ/chồng - Họ tên", "Bố vợ/chồng - Năm sinh", "Bố vợ/chồng - Quê quán", "Bố vợ/chồng - Nghề nghiệp",
                "Bố vợ/chồng - Điện thoại", "Mẹ vợ/chồng - Họ tên", "Mẹ vợ/chồng - Năm sinh", "Mẹ vợ/chồng - Quê quán",
                "Mẹ vợ/chồng - Nghề nghiệp", "Mẹ vợ/chồng - Điện thoại", "Thông tin con", "Trường đào tạo",
                "Chuyên ngành", "Hình thức đào tạo", "Chứng chỉ", "Từ ngày", "Đến ngày", "Ghi chú đào tạo",
                "Tên ngoại ngữ", "Trình độ", "Ngày cấp", "Điểm/kết quả", "Ghi chú ngoại ngữ"
        };
        return java.util.Arrays.copyOf(headers, STAFF_IMPORT_LAST_DATA_COLUMN + 1);
    }

    private Object[] buildStaffExportRow(StaffItemDto item, StaffDetailDto detail, int stt) {
        Object[] values = new Object[STAFF_IMPORT_LAST_DATA_COLUMN + 1];
        values[0] = stt;
        values[1] = item.getFullName();
        values[2] = item.getAliasName();
        values[3] = staffGenderLabel(item.getGender());
        values[4] = formatDate(item.getDateOfBirth());
        values[5] = item.getPhone();
        values[6] = item.getEmail();
        values[7] = item.getGradeId();
        values[8] = staffStatusLabel(item.getStatus());
        values[9] = detail.getNote();
        values[10] = detail.getIdentityCode();
        values[11] = detail.getEthnicityId();
        values[12] = detail.getReligionId();
        values[13] = detail.getNationalityId();
        values[14] = detail.getCccdNo();
        values[15] = formatDate(detail.getCccdIssueDate());
        values[16] = detail.getCccdIssuePlace();
        values[17] = detail.getHealthStatus();
        values[18] = detail.getSocialInsuranceNo();
        values[19] = detail.getPermanentAddress() == null ? null : detail.getPermanentAddress().getFullAddress();
        values[20] = detail.getTemporaryAddress() == null ? null : detail.getTemporaryAddress().getFullAddress();
        values[21] = detail.getBirthPlaceAddress() == null ? null : detail.getBirthPlaceAddress().getFullAddress();
        values[22] = detail.getFatherInfo() == null ? null : detail.getFatherInfo().getFullName();
        values[23] = detail.getFatherInfo() == null ? null : detail.getFatherInfo().getBirthYear();
        values[24] = detail.getFatherInfo() == null ? null : detail.getFatherInfo().getHometown();
        values[25] = detail.getFatherInfo() == null ? null : detail.getFatherInfo().getOccupation();
        values[26] = detail.getFatherInfo() == null ? null : detail.getFatherInfo().getPhone();
        values[27] = detail.getMotherInfo() == null ? null : detail.getMotherInfo().getFullName();
        values[28] = detail.getMotherInfo() == null ? null : detail.getMotherInfo().getBirthYear();
        values[29] = detail.getMotherInfo() == null ? null : detail.getMotherInfo().getHometown();
        values[30] = detail.getMotherInfo() == null ? null : detail.getMotherInfo().getOccupation();
        values[31] = detail.getMotherInfo() == null ? null : detail.getMotherInfo().getPhone();
        values[32] = detail.getSpouseInfo() == null ? null : detail.getSpouseInfo().getFullName();
        values[33] = detail.getSpouseInfo() == null ? null : detail.getSpouseInfo().getBirthYear();
        values[34] = detail.getSpouseInfo() == null ? null : detail.getSpouseInfo().getOccupation();
        values[35] = detail.getSpouseInfo() == null ? null : detail.getSpouseInfo().getPhone();
        values[36] = detail.getSpouseFatherInfo() == null ? null : detail.getSpouseFatherInfo().getFullName();
        values[37] = detail.getSpouseFatherInfo() == null ? null : detail.getSpouseFatherInfo().getBirthYear();
        values[38] = detail.getSpouseFatherInfo() == null ? null : detail.getSpouseFatherInfo().getHometown();
        values[39] = detail.getSpouseFatherInfo() == null ? null : detail.getSpouseFatherInfo().getOccupation();
        values[40] = detail.getSpouseFatherInfo() == null ? null : detail.getSpouseFatherInfo().getPhone();
        values[41] = detail.getSpouseMotherInfo() == null ? null : detail.getSpouseMotherInfo().getFullName();
        values[42] = detail.getSpouseMotherInfo() == null ? null : detail.getSpouseMotherInfo().getBirthYear();
        values[43] = detail.getSpouseMotherInfo() == null ? null : detail.getSpouseMotherInfo().getHometown();
        values[44] = detail.getSpouseMotherInfo() == null ? null : detail.getSpouseMotherInfo().getOccupation();
        values[45] = detail.getSpouseMotherInfo() == null ? null : detail.getSpouseMotherInfo().getPhone();
        values[46] = detail.getChildrenDetail();
        return values;
    }

    private byte[] buildStaffExportPdf(List<StaffItemDto> items, Long unitId) {
        Unit unit = unitId == null ? null : unitRepository.findById(unitId).orElse(null);
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4.rotate(), 24, 24, 20, 20);
            PdfWriter.getInstance(document, outputStream);
            document.open();

            com.lowagie.text.Font titleFont = createPdfFont(16, com.lowagie.text.Font.BOLD);
            com.lowagie.text.Font headerFont = createPdfFont(10, com.lowagie.text.Font.BOLD);
            com.lowagie.text.Font bodyFont = createPdfFont(10, com.lowagie.text.Font.NORMAL);
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

            Paragraph title = new Paragraph("DANH SÁCH CÁN BỘ", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(12f);
            document.add(title);

            PdfPTable table = new PdfPTable(new float[] { 0.7f, 1.4f, 2.5f, 1.2f, 1.3f, 1.5f, 2.0f, 1.5f, 1.0f });
            table.setWidthPercentage(100);
            String[] headers = { "STT", "Mã cán bộ", "Họ tên", "Giới tính", "Ngày sinh", "Điện thoại", "Email", "Trạng thái", "Khối" };
            for (String header : headers) {
                addPdfHeaderCell(table, header, headerFont);
            }

            int stt = 1;
            for (StaffItemDto item : items) {
                addPdfBodyCell(table, String.valueOf(stt++), bodyFont, Element.ALIGN_CENTER);
                addPdfBodyCell(table, item.getStaffCode(), bodyFont, Element.ALIGN_LEFT);
                addPdfBodyCell(table, item.getFullName(), bodyFont, Element.ALIGN_LEFT);
                addPdfBodyCell(table, staffGenderLabel(item.getGender()), bodyFont, Element.ALIGN_CENTER);
                addPdfBodyCell(table, formatDate(item.getDateOfBirth()), bodyFont, Element.ALIGN_CENTER);
                addPdfBodyCell(table, item.getPhone(), bodyFont, Element.ALIGN_LEFT);
                addPdfBodyCell(table, item.getEmail(), bodyFont, Element.ALIGN_LEFT);
                addPdfBodyCell(table, staffStatusLabel(item.getStatus()), bodyFont, Element.ALIGN_CENTER);
                addPdfBodyCell(table, item.getGradeId() == null ? null : String.valueOf(item.getGradeId()), bodyFont, Element.ALIGN_CENTER);
            }

            document.add(table);
            document.close();
            return outputStream.toByteArray();
        } catch (DocumentException | IOException ex) {
            throw new UserMessageException("Không thể xuất PDF cán bộ");
        }
    }

    private void upsertStaffFromExcelRow(Unit unit, Row row, DataFormatter formatter, boolean hasLegacyCodeColumn) {
        String staffCode = hasLegacyCodeColumn
                ? normalizeNullable(readCellText(row.getCell(STAFF_LEGACY_CODE_COLUMN_INDEX), formatter))
                : null;
        String fullName = normalizeNullable(
                readCellText(row.getCell(staffCol(STAFF_COL_FULL_NAME, hasLegacyCodeColumn)), formatter));
        if (!StringUtils.hasText(fullName)) {
            throw new UserMessageException("Họ tên cán bộ không được để trống");
        }

        Staff existing = StringUtils.hasText(staffCode)
                ? staffRepository.findByStaffCode(staffCode).orElse(null)
                : null;

        if (existing == null) {
            StaffCreateRequest request = new StaffCreateRequest();
            request.setStaffCode(resolveImportStaffCode(unit, staffCode));
            request.setFullName(fullName);
            request.setAliasName(normalizeNullable(
                    readCellText(row.getCell(staffCol(STAFF_COL_ALIAS_NAME, hasLegacyCodeColumn)), formatter)));
            request.setIdentityCode(normalizeNullable(
                    readCellText(row.getCell(staffCol(STAFF_COL_IDENTITY_CODE, hasLegacyCodeColumn)), formatter)));
            request.setGender(parseStaffGenderCell(
                    readCellText(row.getCell(staffCol(STAFF_COL_GENDER, hasLegacyCodeColumn)), formatter)));
            request.setDateOfBirth(parseOptionalDateCell(
                    readCellText(row.getCell(staffCol(STAFF_COL_DOB, hasLegacyCodeColumn)), formatter)));
            request.setEthnicityId(parseStaffEthnicityCell(
                    readCellText(row.getCell(staffCol(STAFF_COL_ETHNICITY, hasLegacyCodeColumn)), formatter)));
            request.setReligionId(normalizeNullable(
                    readCellText(row.getCell(staffCol(STAFF_COL_RELIGION, hasLegacyCodeColumn)), formatter)));
            request.setNationalityId(normalizeNullable(
                    readCellText(row.getCell(staffCol(STAFF_COL_NATIONALITY, hasLegacyCodeColumn)), formatter)));
            request.setCccdNo(normalizeNullable(
                    readCellText(row.getCell(staffCol(STAFF_COL_CCCD_NO, hasLegacyCodeColumn)), formatter)));
            request.setCccdIssueDate(parseOptionalDateCell(
                    readCellText(row.getCell(staffCol(STAFF_COL_CCCD_ISSUE_DATE, hasLegacyCodeColumn)), formatter)));
            request.setCccdIssuePlace(normalizeNullable(
                    readCellText(row.getCell(staffCol(STAFF_COL_CCCD_ISSUE_PLACE, hasLegacyCodeColumn)), formatter)));
            request.setPhone(normalizeNullable(
                    readCellText(row.getCell(staffCol(STAFF_COL_PHONE, hasLegacyCodeColumn)), formatter)));
            request.setEmail(normalizeNullable(
                    readCellText(row.getCell(staffCol(STAFF_COL_EMAIL, hasLegacyCodeColumn)), formatter)));
            request.setHealthStatus(normalizeNullable(
                    readCellText(row.getCell(staffCol(STAFF_COL_HEALTH_STATUS, hasLegacyCodeColumn)), formatter)));
            request.setSocialInsuranceNo(normalizeNullable(readCellText(
                    row.getCell(staffCol(STAFF_COL_SOCIAL_INSURANCE_NO, hasLegacyCodeColumn)), formatter)));
            request.setGradeId(parseOptionalLongCell(
                    readCellText(row.getCell(staffCol(STAFF_COL_GRADE_ID, hasLegacyCodeColumn)), formatter)));
            request.setStatus(parseStaffStatusCell(
                    readCellText(row.getCell(staffCol(STAFF_COL_STATUS, hasLegacyCodeColumn)), formatter)));
            request.setNote(normalizeNullable(
                    readCellText(row.getCell(staffCol(STAFF_COL_NOTE, hasLegacyCodeColumn)), formatter)));
            request.setPermanentAddress(buildAddressFromFullAddressCell(
                    readCellText(row.getCell(staffCol(STAFF_COL_PERMANENT_ADDRESS, hasLegacyCodeColumn)), formatter)));
            request.setTemporaryAddress(buildAddressFromFullAddressCell(
                    readCellText(row.getCell(staffCol(STAFF_COL_TEMPORARY_ADDRESS, hasLegacyCodeColumn)), formatter)));
            request.setBirthPlaceAddress(buildAddressFromFullAddressCell(readCellText(
                    row.getCell(staffCol(STAFF_COL_BIRTH_PLACE_ADDRESS, hasLegacyCodeColumn)), formatter)));
            request.setFatherInfo(buildFamilyMemberRequest(
                    readCellText(row.getCell(staffCol(STAFF_COL_FATHER_NAME, hasLegacyCodeColumn)), formatter),
                    readCellText(row.getCell(staffCol(STAFF_COL_FATHER_BIRTH_YEAR, hasLegacyCodeColumn)), formatter),
                    readCellText(row.getCell(staffCol(STAFF_COL_FATHER_HOMETOWN, hasLegacyCodeColumn)), formatter),
                    readCellText(row.getCell(staffCol(STAFF_COL_FATHER_OCCUPATION, hasLegacyCodeColumn)), formatter),
                    readCellText(row.getCell(staffCol(STAFF_COL_FATHER_PHONE, hasLegacyCodeColumn)), formatter)));
            request.setMotherInfo(buildFamilyMemberRequest(
                    readCellText(row.getCell(staffCol(STAFF_COL_MOTHER_NAME, hasLegacyCodeColumn)), formatter),
                    readCellText(row.getCell(staffCol(STAFF_COL_MOTHER_BIRTH_YEAR, hasLegacyCodeColumn)), formatter),
                    readCellText(row.getCell(staffCol(STAFF_COL_MOTHER_HOMETOWN, hasLegacyCodeColumn)), formatter),
                    readCellText(row.getCell(staffCol(STAFF_COL_MOTHER_OCCUPATION, hasLegacyCodeColumn)), formatter),
                    readCellText(row.getCell(staffCol(STAFF_COL_MOTHER_PHONE, hasLegacyCodeColumn)), formatter)));
            request.setSpouseInfo(buildFamilyMemberRequest(
                    readCellText(row.getCell(staffCol(STAFF_COL_SPOUSE_NAME, hasLegacyCodeColumn)), formatter),
                    readCellText(row.getCell(staffCol(STAFF_COL_SPOUSE_BIRTH_YEAR, hasLegacyCodeColumn)), formatter),
                    null,
                    readCellText(row.getCell(staffCol(STAFF_COL_SPOUSE_OCCUPATION, hasLegacyCodeColumn)), formatter),
                    readCellText(row.getCell(staffCol(STAFF_COL_SPOUSE_PHONE, hasLegacyCodeColumn)), formatter)));
            request.setSpouseFatherInfo(buildFamilyMemberRequest(
                    readCellText(row.getCell(staffCol(STAFF_COL_SPOUSE_FATHER_NAME, hasLegacyCodeColumn)), formatter),
                    readCellText(row.getCell(staffCol(STAFF_COL_SPOUSE_FATHER_BIRTH_YEAR, hasLegacyCodeColumn)),
                            formatter),
                    readCellText(row.getCell(staffCol(STAFF_COL_SPOUSE_FATHER_HOMETOWN, hasLegacyCodeColumn)),
                            formatter),
                    readCellText(row.getCell(staffCol(STAFF_COL_SPOUSE_FATHER_OCCUPATION, hasLegacyCodeColumn)),
                            formatter),
                    readCellText(row.getCell(staffCol(STAFF_COL_SPOUSE_FATHER_PHONE, hasLegacyCodeColumn)),
                            formatter)));
            request.setSpouseMotherInfo(buildFamilyMemberRequest(
                    readCellText(row.getCell(staffCol(STAFF_COL_SPOUSE_MOTHER_NAME, hasLegacyCodeColumn)), formatter),
                    readCellText(row.getCell(staffCol(STAFF_COL_SPOUSE_MOTHER_BIRTH_YEAR, hasLegacyCodeColumn)),
                            formatter),
                    readCellText(row.getCell(staffCol(STAFF_COL_SPOUSE_MOTHER_HOMETOWN, hasLegacyCodeColumn)),
                            formatter),
                    readCellText(row.getCell(staffCol(STAFF_COL_SPOUSE_MOTHER_OCCUPATION, hasLegacyCodeColumn)),
                            formatter),
                    readCellText(row.getCell(staffCol(STAFF_COL_SPOUSE_MOTHER_PHONE, hasLegacyCodeColumn)),
                            formatter)));
            request.setChildrenDetail(normalizeNullable(
                    readCellText(row.getCell(staffCol(STAFF_COL_CHILDREN_DETAIL, hasLegacyCodeColumn)), formatter)));
            request.setUnitId(unit.getId());
            StaffDetailDto created = create(request);
            syncImportedEducationSections(created.getId(), row, formatter, hasLegacyCodeColumn);
            return;
        }

        StaffUpdateRequest request = new StaffUpdateRequest();
        request.setFullName(fullName);
        request.setAliasName(normalizeNullable(
                readCellText(row.getCell(staffCol(STAFF_COL_ALIAS_NAME, hasLegacyCodeColumn)), formatter)));
        request.setIdentityCode(normalizeNullable(
                readCellText(row.getCell(staffCol(STAFF_COL_IDENTITY_CODE, hasLegacyCodeColumn)), formatter)));
        request.setGender(parseStaffGenderCell(
                readCellText(row.getCell(staffCol(STAFF_COL_GENDER, hasLegacyCodeColumn)), formatter)));
        request.setDateOfBirth(parseOptionalDateCell(
                readCellText(row.getCell(staffCol(STAFF_COL_DOB, hasLegacyCodeColumn)), formatter)));
        request.setEthnicityId(parseStaffEthnicityCell(
                readCellText(row.getCell(staffCol(STAFF_COL_ETHNICITY, hasLegacyCodeColumn)), formatter)));
        request.setReligionId(normalizeNullable(
                readCellText(row.getCell(staffCol(STAFF_COL_RELIGION, hasLegacyCodeColumn)), formatter)));
        request.setNationalityId(normalizeNullable(
                readCellText(row.getCell(staffCol(STAFF_COL_NATIONALITY, hasLegacyCodeColumn)), formatter)));
        request.setCccdNo(normalizeNullable(
                readCellText(row.getCell(staffCol(STAFF_COL_CCCD_NO, hasLegacyCodeColumn)), formatter)));
        request.setCccdIssueDate(parseOptionalDateCell(
                readCellText(row.getCell(staffCol(STAFF_COL_CCCD_ISSUE_DATE, hasLegacyCodeColumn)), formatter)));
        request.setCccdIssuePlace(normalizeNullable(
                readCellText(row.getCell(staffCol(STAFF_COL_CCCD_ISSUE_PLACE, hasLegacyCodeColumn)), formatter)));
        request.setPhone(normalizeNullable(
                readCellText(row.getCell(staffCol(STAFF_COL_PHONE, hasLegacyCodeColumn)), formatter)));
        request.setEmail(normalizeNullable(
                readCellText(row.getCell(staffCol(STAFF_COL_EMAIL, hasLegacyCodeColumn)), formatter)));
        request.setHealthStatus(normalizeNullable(
                readCellText(row.getCell(staffCol(STAFF_COL_HEALTH_STATUS, hasLegacyCodeColumn)), formatter)));
        request.setSocialInsuranceNo(normalizeNullable(
                readCellText(row.getCell(staffCol(STAFF_COL_SOCIAL_INSURANCE_NO, hasLegacyCodeColumn)), formatter)));
        request.setGradeId(parseOptionalLongCell(
                readCellText(row.getCell(staffCol(STAFF_COL_GRADE_ID, hasLegacyCodeColumn)), formatter)));
        request.setStatus(parseStaffStatusCell(
                readCellText(row.getCell(staffCol(STAFF_COL_STATUS, hasLegacyCodeColumn)), formatter)));
        request.setNote(
                normalizeNullable(readCellText(row.getCell(staffCol(STAFF_COL_NOTE, hasLegacyCodeColumn)), formatter)));
        request.setPermanentAddress(buildAddressFromFullAddressCell(
                readCellText(row.getCell(staffCol(STAFF_COL_PERMANENT_ADDRESS, hasLegacyCodeColumn)), formatter)));
        request.setTemporaryAddress(buildAddressFromFullAddressCell(
                readCellText(row.getCell(staffCol(STAFF_COL_TEMPORARY_ADDRESS, hasLegacyCodeColumn)), formatter)));
        request.setBirthPlaceAddress(buildAddressFromFullAddressCell(
                readCellText(row.getCell(staffCol(STAFF_COL_BIRTH_PLACE_ADDRESS, hasLegacyCodeColumn)), formatter)));
        request.setFatherInfo(buildFamilyMemberRequest(
                readCellText(row.getCell(staffCol(STAFF_COL_FATHER_NAME, hasLegacyCodeColumn)), formatter),
                readCellText(row.getCell(staffCol(STAFF_COL_FATHER_BIRTH_YEAR, hasLegacyCodeColumn)), formatter),
                readCellText(row.getCell(staffCol(STAFF_COL_FATHER_HOMETOWN, hasLegacyCodeColumn)), formatter),
                readCellText(row.getCell(staffCol(STAFF_COL_FATHER_OCCUPATION, hasLegacyCodeColumn)), formatter),
                readCellText(row.getCell(staffCol(STAFF_COL_FATHER_PHONE, hasLegacyCodeColumn)), formatter)));
        request.setMotherInfo(buildFamilyMemberRequest(
                readCellText(row.getCell(staffCol(STAFF_COL_MOTHER_NAME, hasLegacyCodeColumn)), formatter),
                readCellText(row.getCell(staffCol(STAFF_COL_MOTHER_BIRTH_YEAR, hasLegacyCodeColumn)), formatter),
                readCellText(row.getCell(staffCol(STAFF_COL_MOTHER_HOMETOWN, hasLegacyCodeColumn)), formatter),
                readCellText(row.getCell(staffCol(STAFF_COL_MOTHER_OCCUPATION, hasLegacyCodeColumn)), formatter),
                readCellText(row.getCell(staffCol(STAFF_COL_MOTHER_PHONE, hasLegacyCodeColumn)), formatter)));
        request.setSpouseInfo(buildFamilyMemberRequest(
                readCellText(row.getCell(staffCol(STAFF_COL_SPOUSE_NAME, hasLegacyCodeColumn)), formatter),
                readCellText(row.getCell(staffCol(STAFF_COL_SPOUSE_BIRTH_YEAR, hasLegacyCodeColumn)), formatter),
                null,
                readCellText(row.getCell(staffCol(STAFF_COL_SPOUSE_OCCUPATION, hasLegacyCodeColumn)), formatter),
                readCellText(row.getCell(staffCol(STAFF_COL_SPOUSE_PHONE, hasLegacyCodeColumn)), formatter)));
        request.setSpouseFatherInfo(buildFamilyMemberRequest(
                readCellText(row.getCell(staffCol(STAFF_COL_SPOUSE_FATHER_NAME, hasLegacyCodeColumn)), formatter),
                readCellText(row.getCell(staffCol(STAFF_COL_SPOUSE_FATHER_BIRTH_YEAR, hasLegacyCodeColumn)), formatter),
                readCellText(row.getCell(staffCol(STAFF_COL_SPOUSE_FATHER_HOMETOWN, hasLegacyCodeColumn)), formatter),
                readCellText(row.getCell(staffCol(STAFF_COL_SPOUSE_FATHER_OCCUPATION, hasLegacyCodeColumn)), formatter),
                readCellText(row.getCell(staffCol(STAFF_COL_SPOUSE_FATHER_PHONE, hasLegacyCodeColumn)), formatter)));
        request.setSpouseMotherInfo(buildFamilyMemberRequest(
                readCellText(row.getCell(staffCol(STAFF_COL_SPOUSE_MOTHER_NAME, hasLegacyCodeColumn)), formatter),
                readCellText(row.getCell(staffCol(STAFF_COL_SPOUSE_MOTHER_BIRTH_YEAR, hasLegacyCodeColumn)), formatter),
                readCellText(row.getCell(staffCol(STAFF_COL_SPOUSE_MOTHER_HOMETOWN, hasLegacyCodeColumn)), formatter),
                readCellText(row.getCell(staffCol(STAFF_COL_SPOUSE_MOTHER_OCCUPATION, hasLegacyCodeColumn)), formatter),
                readCellText(row.getCell(staffCol(STAFF_COL_SPOUSE_MOTHER_PHONE, hasLegacyCodeColumn)), formatter)));
        request.setChildrenDetail(normalizeNullable(
                readCellText(row.getCell(staffCol(STAFF_COL_CHILDREN_DETAIL, hasLegacyCodeColumn)), formatter)));
        request.setUnitId(unit.getId());
        StaffDetailDto updated = update(existing.getId(), request);
        syncImportedEducationSections(updated.getId(), row, formatter, hasLegacyCodeColumn);
    }

    private void syncImportedEducationSections(Long staffId, Row row, DataFormatter formatter,
            boolean hasLegacyCodeColumn) {
        Staff staff = findStaff(staffId);
        replaceImportedEducationByType(staff, EDUCATION_TYPE_TRAINING,
                normalizeNullable(
                        readCellText(row.getCell(staffCol(STAFF_COL_TRAINING_SCHOOL, hasLegacyCodeColumn)), formatter)),
                education -> {
                    education.setSchoolName(normalizeNullable(readCellText(
                            row.getCell(staffCol(STAFF_COL_TRAINING_SCHOOL, hasLegacyCodeColumn)), formatter)));
                    education.setMajor(normalizeNullable(readCellText(
                            row.getCell(staffCol(STAFF_COL_TRAINING_MAJOR, hasLegacyCodeColumn)), formatter)));
                    education.setTrainingForm(normalizeNullable(readCellText(
                            row.getCell(staffCol(STAFF_COL_TRAINING_FORM, hasLegacyCodeColumn)), formatter)));
                    education.setCertificate(normalizeNullable(readCellText(
                            row.getCell(staffCol(STAFF_COL_TRAINING_CERTIFICATE, hasLegacyCodeColumn)), formatter)));
                    education.setFromDate(parseOptionalDateCell(readCellText(
                            row.getCell(staffCol(STAFF_COL_TRAINING_FROM_DATE, hasLegacyCodeColumn)), formatter)));
                    education.setToDate(parseOptionalDateCell(readCellText(
                            row.getCell(staffCol(STAFF_COL_TRAINING_TO_DATE, hasLegacyCodeColumn)), formatter)));
                    education.setNote(normalizeNullable(readCellText(
                            row.getCell(staffCol(STAFF_COL_TRAINING_NOTE, hasLegacyCodeColumn)), formatter)));
                    education.setFrameworkLevel(null);
                    education.setScore(null);
                });

        replaceImportedEducationByType(staff, EDUCATION_TYPE_FOREIGN_LANGUAGE,
                normalizeNullable(readCellText(
                        row.getCell(staffCol(STAFF_COL_FOREIGN_LANGUAGE_NAME, hasLegacyCodeColumn)), formatter)),
                education -> {
                    education.setSchoolName(normalizeNullable(readCellText(
                            row.getCell(staffCol(STAFF_COL_FOREIGN_LANGUAGE_NAME, hasLegacyCodeColumn)), formatter)));
                    education.setFrameworkLevel(normalizeNullable(readCellText(
                            row.getCell(staffCol(STAFF_COL_FOREIGN_LANGUAGE_LEVEL, hasLegacyCodeColumn)), formatter)));
                    education.setFromDate(parseOptionalDateCell(readCellText(
                            row.getCell(staffCol(STAFF_COL_FOREIGN_LANGUAGE_ISSUE_DATE, hasLegacyCodeColumn)),
                            formatter)));
                    education.setScore(normalizeNullable(readCellText(
                            row.getCell(staffCol(STAFF_COL_FOREIGN_LANGUAGE_SCORE, hasLegacyCodeColumn)), formatter)));
                    education.setNote(normalizeNullable(readCellText(
                            row.getCell(staffCol(STAFF_COL_FOREIGN_LANGUAGE_NOTE, hasLegacyCodeColumn)), formatter)));
                    education.setMajor(null);
                    education.setTrainingForm(null);
                    education.setCertificate(null);
                    education.setToDate(null);
                });
    }

    private void replaceImportedEducationByType(Staff staff, String educationType, String triggerValue,
            java.util.function.Consumer<StaffEducation> applier) {
        List<StaffEducation> sameTypeItems = staffEducationRepository.findByStaffId(staff.getId()).stream()
                .filter(item -> educationType.equals(item.getEducationType()))
                .toList();
        if (!StringUtils.hasText(triggerValue)) {
            if (!sameTypeItems.isEmpty()) {
                staffEducationRepository.deleteAll(sameTypeItems);
            }
            return;
        }
        StaffEducation education = sameTypeItems.isEmpty() ? new StaffEducation() : sameTypeItems.get(0);
        education.setStaff(staff);
        education.setEducationType(educationType);
        applier.accept(education);
        staffEducationRepository.save(education);
        if (sameTypeItems.size() > 1) {
            staffEducationRepository.deleteAll(sameTypeItems.subList(1, sameTypeItems.size()));
        }
    }

    private byte[] buildStaffImportErrorFile(Workbook workbook, Sheet sheet, Map<Integer, String> rowErrors,
            int headerRowIndex) {
        CellStyle headerStyle = createExcelHeaderStyle(workbook);
        CellStyle bodyStyle = createExcelBodyStyle(workbook);
        Row headerRow = sheet.getRow(headerRowIndex);
        if (headerRow == null) {
            headerRow = sheet.createRow(headerRowIndex);
        }
        int resultColumnIndex = headerRow == null || headerRow.getLastCellNum() < 0
                ? STAFF_IMPORT_LAST_DATA_COLUMN + 1
                : headerRow.getLastCellNum();
        int reasonColumnIndex = resultColumnIndex + 1;
        createCell(headerRow, resultColumnIndex, "Kết quả", headerStyle);
        createCell(headerRow, reasonColumnIndex, "Lý do lỗi", headerStyle);
        for (Map.Entry<Integer, String> entry : rowErrors.entrySet()) {
            Row row = sheet.getRow(entry.getKey());
            createCell(row, resultColumnIndex, "Thất bại", bodyStyle);
            createCell(row, reasonColumnIndex, entry.getValue(), bodyStyle);
        }
        autosize(sheet, reasonColumnIndex + 1);
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException ex) {
            throw new UserMessageException("Không thể tạo file lỗi import cán bộ");
        }
    }

    private void validateExcelFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new UserMessageException("File import không được để trống");
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

    private int findStaffImportHeaderRow(Sheet sheet, DataFormatter formatter) {
        int maxCheck = Math.min(sheet.getLastRowNum(), 12);
        for (int rowIndex = 0; rowIndex <= maxCheck; rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                continue;
            }
            String first = normalizeLookupKey(readCellText(row.getCell(0), formatter));
            String second = normalizeLookupKey(readCellText(row.getCell(1), formatter));
            if ("STT".equals(first) && ("HO TEN *".equals(second) || "MA CAN BO".equals(second)
                    || "HO TEN".equals(second))) {
                return rowIndex;
            }
        }
        return STAFF_IMPORT_HEADER_ROW_INDEX;
    }

    private boolean isLegacyStaffCodeColumn(Row headerRow, DataFormatter formatter) {
        if (headerRow == null) {
            return false;
        }
        String second = normalizeLookupKey(readCellText(headerRow.getCell(1), formatter));
        return "MA CAN BO".equals(second);
    }

    private int staffCol(int baseColumn, boolean hasLegacyCodeColumn) {
        return hasLegacyCodeColumn ? baseColumn + 1 : baseColumn;
    }

    private void fillRowWithStyle(Row row, int fromColumn, int toColumn, CellStyle style) {
        for (int i = fromColumn; i <= toColumn; i++) {
            createCell(row, i, "", style);
        }
    }

    private LocalDate parseOptionalDateCell(String value) {
        String normalized = normalizeNullable(value);
        if (normalized == null) {
            return null;
        }
        try {
            if (normalized.contains("/")) {
                return LocalDate.parse(normalized, DATE_FORMATTER);
            }
            return LocalDate.parse(normalized);
        } catch (Exception ex) {
            throw new UserMessageException("Ngày tháng không hợp lệ: " + normalized);
        }
    }

    private Long parseOptionalLongCell(String value) {
        String normalized = normalizeNullable(value);
        if (normalized == null) {
            return null;
        }
        try {
            return Long.valueOf(normalized);
        } catch (NumberFormatException ex) {
            throw new UserMessageException("Giá trị ID không hợp lệ: " + normalized);
        }
    }

    private Integer parseOptionalIntegerCell(String value, String messagePrefix) {
        String normalized = normalizeNullable(value);
        if (normalized == null) {
            return null;
        }
        try {
            return Integer.valueOf(normalized);
        } catch (NumberFormatException ex) {
            throw new UserMessageException(messagePrefix + ": " + normalized);
        }
    }

    private StaffAddressRequest buildAddressFromFullAddressCell(String fullAddressText) {
        String fullAddress = normalizeNullable(fullAddressText);
        if (fullAddress == null) {
            return null;
        }
        StaffAddressRequest request = new StaffAddressRequest();
        request.setFullAddress(fullAddress);
        return request;
    }

    private StaffFamilyMemberRequest buildFamilyMemberRequest(String fullName, String birthYearText, String hometown,
            String occupation, String phone) {
        StaffFamilyMemberRequest request = new StaffFamilyMemberRequest();
        request.setFullName(normalizeNullable(fullName));
        request.setBirthYear(parseOptionalIntegerCell(birthYearText, "Năm sinh không hợp lệ"));
        request.setHometown(normalizeNullable(hometown));
        request.setOccupation(normalizeNullable(occupation));
        request.setPhone(normalizeNullable(phone));
        return isFamilyMemberEmpty(request) ? null : request;
    }

    private String parseStaffGenderCell(String value) {
        String normalized = normalizeNullable(value);
        if (normalized == null) {
            return null;
        }
        String key = normalizeLookupKey(normalized);
        return switch (key) {
            case "NAM", "MALE", "M", "0" -> "MALE";
            case "NU", "FEMALE", "F", "1" -> "FEMALE";
            case "KHAC", "OTHER", "O", "2" -> "OTHER";
            default -> normalized.toUpperCase(Locale.ROOT);
        };
    }

    private String parseStaffStatusCell(String value) {
        String normalized = normalizeNullable(value);
        if (normalized == null) {
            return null;
        }
        String key = normalizeLookupKey(normalized);
        return switch (key) {
            case "DANG LAM VIEC", "ACTIVE", "1" -> "ACTIVE";
            case "NGUNG HOAT DONG", "INACTIVE", "0" -> "INACTIVE";
            default -> throw new UserMessageException("Trạng thái cán bộ không hợp lệ");
        };
    }

    private String staffGenderLabel(String gender) {
        if (!StringUtils.hasText(gender)) {
            return null;
        }
        return switch (gender.toUpperCase(Locale.ROOT)) {
            case "MALE" -> "Nam";
            case "FEMALE" -> "Nữ";
            case "OTHER" -> "Khác";
            default -> gender;
        };
    }

    private String staffStatusLabel(String status) {
        if (!StringUtils.hasText(status)) {
            return null;
        }
        return "ACTIVE".equalsIgnoreCase(status) ? "Đang làm việc"
                : "INACTIVE".equalsIgnoreCase(status) ? "Ngừng hoạt động" : status;
    }

    private String parseStaffEthnicityCell(String value) {
        String normalized = normalizeNullable(value);
        if (normalized == null) {
            return null;
        }
        String key = normalizeLookupKey(normalized);
        return STAFF_ETHNICITY_OPTIONS.stream()
                .filter(item -> normalizeLookupKey(item).equals(key))
                .findFirst()
                .orElseThrow(() -> new UserMessageException("Dân tộc không hợp lệ"));
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

    private String resolveImportStaffCode(Unit unit, String inputCode) {
        if (StringUtils.hasText(inputCode)) {
            return inputCode;
        }
        try {
            return staffCodeGeneratorService.generateStaffCode(unit.getId(), LocalDate.now().getYear());
        } catch (RuntimeException ex) {
            throw new UserMessageException("Không thể tự động sinh mã cán bộ: " + ex.getMessage());
        }
    }

    private String formatDate(LocalDate value) {
        return value == null ? null : value.format(DATE_FORMATTER);
    }

    private String buildExportInfoLine() {
        String exportTime = LocalDateTime.now().format(EXPORT_TIME_FORMATTER);
        String username = SecurityUtils.getCurrentUsername();
        return "Thời gian tải: " + exportTime + " | Người tải: " + username;
    }

    private void createStaffEthnicitySheet(Workbook workbook) {
        Sheet sheet = workbook.createSheet("DanToc");
        CellStyle headerStyle = createExcelHeaderStyle(workbook);
        CellStyle bodyStyle = createExcelBodyStyle(workbook);

        createCell(sheet.createRow(0), 0, "Danh sách dân tộc hợp lệ", headerStyle);
        createCell(sheet.createRow(1), 0,
                "Cột Dân tộc trong sheet CanBo chỉ được nhập đúng một trong các giá trị dưới đây.",
                bodyStyle);

        Row headerRow = sheet.createRow(3);
        createCell(headerRow, 0, "STT", headerStyle);
        createCell(headerRow, 1, "Tên dân tộc", headerStyle);

        int rowIndex = 4;
        for (int i = 0; i < STAFF_ETHNICITY_OPTIONS.size(); i++) {
            Row row = sheet.createRow(rowIndex++);
            createCell(row, 0, i + 1, bodyStyle);
            createCell(row, 1, STAFF_ETHNICITY_OPTIONS.get(i), bodyStyle);
        }

        autosize(sheet, 2);
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
            if (Files.exists(Path.of(fontPath))) {
                BaseFont baseFont = BaseFont.createFont(fontPath, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
                return new com.lowagie.text.Font(baseFont, size, com.lowagie.text.Font.NORMAL);
            }
        } catch (Exception ignored) {
        }
        return com.lowagie.text.FontFactory.getFont(EXPORT_FONT_NAME, BaseFont.IDENTITY_H, true, size, style);
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

    private CellStyle createExcelGuideStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setWrapText(true);
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        Font font = workbook.createFont();
        font.setFontHeightInPoints((short) 10);
        font.setItalic(true);
        font.setFontName(EXPORT_FONT_NAME);
        style.setFont(font);
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
}
