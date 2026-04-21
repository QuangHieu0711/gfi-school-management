package com.gfi.backend.services.implement;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Supplier;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.CellStyle;
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
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.gfi.backend.controllers.exceptions.UserMessageException;
import com.gfi.backend.models.dtos.common.PageRequestDto;
import com.gfi.backend.models.dtos.common.PageResponseDto;
import com.gfi.backend.models.dtos.common.LookupItemDto;
import com.gfi.backend.models.dtos.user.UserCreateRequest;
import com.gfi.backend.models.dtos.user.UserDetailDto;
import com.gfi.backend.models.dtos.user.UserFilterDto;
import com.gfi.backend.models.dtos.user.UserListItemDto;
import com.gfi.backend.models.dtos.user.UserUpdateRequest;
import com.gfi.backend.models.enums.ExportType;
import com.gfi.backend.models.entities.Role;
import com.gfi.backend.models.entities.Unit;
import com.gfi.backend.models.entities.User;
import com.gfi.backend.models.global.CommonErrorCode;
import com.gfi.backend.models.mappers.UserMapper;
import com.gfi.backend.repositories.RoleRepository;
import com.gfi.backend.repositories.StaffRepository;
import com.gfi.backend.repositories.UnitRepository;
import com.gfi.backend.repositories.UserRepository;
import com.gfi.backend.repositories.specifications.UserSpecification;
import com.gfi.backend.services.interfaces.UserService;
import com.gfi.backend.models.entities.Staff;
import com.gfi.backend.utils.PageableUtils;
import com.gfi.backend.utils.PasswordUtils;
import com.gfi.backend.utils.SecurityUtils;
import com.gfi.backend.utils.SecurityContextUtils;
import com.gfi.backend.utils.ScopeFilterUtils;
import com.gfi.backend.models.security.UserScopes;
import com.gfi.backend.models.security.FeatureKey;
import com.gfi.backend.models.security.ResolvedScope;
import com.gfi.backend.models.enums.ActionType;
import com.gfi.backend.models.enums.ScopeType;
import com.gfi.backend.repositories.RoleAssignmentPermissionRepository;
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

import lombok.RequiredArgsConstructor;

/**
 * Service xử lý logic quản lý người dùng.
 * 
 * Trách nhiệm tách biệt:
 * - Logic query: UserSpecification
 * - Logic mapping: UserMapper
 * - Load quan hệ & validate: private helpers
 * - Security: SecurityUtils
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UnitRepository unitRepository;
    private final StaffRepository staffRepository;
    private final RoleAssignmentPermissionRepository roleAssignmentPermissionRepository;
    private final UserSpecification userSpecification;
    private final UserMapper userMapper;
    
    // Feature key cho phân quyền - match với DB (ACCOUNT_MANAGEMENT)
    private static final String FEATURE = FeatureKey.ACCOUNT_MANAGEMENT.getCode();
    private static final String STAFF_FEATURE = FeatureKey.STAFF_PROFILE.getCode();
    private static final String EXPORT_FONT_NAME = "Times New Roman";
    private static final String TIMES_FONT_REGULAR_PATH = "C:/Windows/Fonts/times.ttf";
    private static final String TIMES_FONT_BOLD_PATH = "C:/Windows/Fonts/timesbd.ttf";
    private static final String TIMES_FONT_ITALIC_PATH = "C:/Windows/Fonts/timesi.ttf";
    private static final DateTimeFormatter EXPORT_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    /**
     * Lấy danh sách unit options cho form tạo người dùng.
     * Chỉ trả về unit mà user hiện tại có quyền tạo.
     * - Nếu unrestricted (ALL scope): trả về tất cả units có status=1
     * - Nếu restricted: trả về units từ allowed unit IDs
     */
    @Override
    @Transactional(readOnly = true)
    public List<LookupItemDto> getUnitOptionsForCreateUser() {
        List<ResolvedScope> allowedScopes = resolveScopesForCreateUserOptions();
        
        // Check if user has unrestricted access
        boolean isUnrestricted = allowedScopes.stream().anyMatch(ResolvedScope::isUnrestricted);

        List<Unit> units;
        if (isUnrestricted) {
            // Unrestricted: get all active, non-deleted units
            units = unitRepository.findByStatusAndDeletedFlagOrderByName(1, 0);
        } else {
            // Restricted: get units from allowed IDs (also filter by status and deleted flag)
            List<Long> allowedUnitIds = allowedScopes.stream()
                    .flatMap(rs -> rs.getScopeIds().stream())
                    .toList();
            units = unitRepository.findByIdInAndStatusAndDeletedFlagOrderByName(allowedUnitIds, 1, 0);
        }

        // Map to LookupItemDto
        return units.stream()
                .map(unit -> LookupItemDto.builder()
                        .id(unit.getId())
                        .name(unit.getName())
                        .build())
                .toList();
    }

    private void ensureAccessForCreateUserOptions() {
        tryPrimaryThenFallback(
                () -> {
                    ScopeFilterUtils.checkAccess(FEATURE);
                    return null;
                },
                () -> {
                    ScopeFilterUtils.checkAccess(STAFF_FEATURE);
                    return null;
                });
    }

    private List<ResolvedScope> resolveScopesForCreateUserOptions() {
        return tryPrimaryThenFallback(
                () -> {
                    ScopeFilterUtils.checkAccess(FEATURE);
                    return ScopeFilterUtils.getScopesForQuery(FEATURE, ActionType.ADD);
                },
                () -> {
                    ScopeFilterUtils.checkAccess(STAFF_FEATURE);
                    return ScopeFilterUtils.getScopesForQuery(STAFF_FEATURE, ActionType.ADD);
                });
    }

    private <T> T tryPrimaryThenFallback(Supplier<T> primary, Supplier<T> fallback) {
        try {
            return primary.get();
        } catch (AccessDeniedException primaryException) {
            try {
                return fallback.get();
            } catch (AccessDeniedException fallbackException) {
                throw primaryException;
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<LookupItemDto> getRoleOptionsForCreateUser() {
        ensureAccessForCreateUserOptions();

        UserScopes userScopes = SecurityContextUtils.getCurrentUserScopes()
                .orElseThrow(() -> new UserMessageException(CommonErrorCode.ACCESS_DENIED));

        List<Role> roles = roleAssignmentPermissionRepository
                .findAssignableRolesForCreate(userScopes.getRoleId());

        return roles.stream()
                .map(role -> LookupItemDto.builder()
                        .id(role.getId())
                        .name(role.getRoleName())
                        .build())
                .toList();
    }

    // Danh sách & tìm kiếm có phân trang
    @Override
    @Transactional(readOnly = true)
    public PageResponseDto<UserListItemDto, UserFilterDto> search(PageRequestDto<UserFilterDto> request) {
        UserFilterDto filter = request.getFilter() == null ? new UserFilterDto() : request.getFilter();
        int pageSize = normalizePageSize(request.getPageSize());
        int pageNow = normalizePageNow(request.getPageNow());
        Pageable pageable = PageableUtils.newestFirst(pageNow, pageSize);

        UserFilterDto scopedFilter = applyViewScopeToFilter(filter);
        Page<User> page = userRepository.findAll(userSpecification.buildSpecification(scopedFilter), pageable);

        List<UserListItemDto> items = page.getContent().stream()
                .map(userMapper::toListItemDto)
                .toList();

        return PageResponseDto.<UserListItemDto, UserFilterDto>builder()
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
    public byte[] export(PageRequestDto<UserFilterDto> request, ExportType exportType) {
        UserFilterDto filter = request == null || request.getFilter() == null ? new UserFilterDto() : request.getFilter();
        UserFilterDto scopedFilter = applyViewScopeToFilter(filter);
        List<UserListItemDto> items = userRepository
                .findAll(userSpecification.buildSpecification(scopedFilter), Sort.by(Sort.Direction.DESC, "id"))
                .stream()
                .map(userMapper::toListItemDto)
                .toList();

        return switch (exportType) {
            case EXCEL -> exportUsersExcel(items);
            case PDF -> exportUsersPdf(items);
        };
    }

    // Chi tiết người dùng theo ID
    @Override
    @Transactional(readOnly = true)
    public UserDetailDto getById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserMessageException(CommonErrorCode.USER_NOT_FOUND));
        
        // Enforce scope: validate user's unit is within allowed scopes
        Long unitId = user.getUnitId();
        if (unitId != null) {
            ScopeFilterUtils.validateAccess(FEATURE, ActionType.VIEW, ScopeType.UNIT, unitId);
        }
        
        return userMapper.toDetailDto(user);
    }

    // Tạo mới người dùng
    @Override
    @Transactional
    public UserDetailDto create(UserCreateRequest request) {
        // Step 1: Check functional permission
        ScopeFilterUtils.checkAccess(FEATURE);

        // Step 2: Get current user scopes (already loaded by UserScopesLoadingFilter)
        UserScopes userScopes = SecurityContextUtils.getCurrentUserScopes()
                .orElseThrow(() -> new UserMessageException(CommonErrorCode.ACCESS_DENIED));
        
        // Step 3: Get allowed unit scopes
        List<ResolvedScope> allowedScopes = ScopeFilterUtils.getScopesForQuery(FEATURE, ActionType.ADD);
        boolean isUnrestricted = allowedScopes.stream().anyMatch(ResolvedScope::isUnrestricted);

        // Step 4: Handle unit assignment
        Long unitIdToAssign = request.getUnitId();
        
        // If SCHOOL_ADMIN, auto-assign current user's unit if not specified
        if ("SCHOOL_ADMIN".equals(userScopes.getRoleCode())) {
            if (unitIdToAssign == null) {
                // No unit specified, use current user's unit from scope
                if (!isUnrestricted && !allowedScopes.isEmpty()) {
                    unitIdToAssign = allowedScopes.get(0).getScopeIds().stream().findFirst().orElse(null);
                }
            } else {
                // Unit specified, validate it's within SCHOOL_ADMIN's allowed scope
                List<Long> allowedUnitIds = allowedScopes.stream()
                        .flatMap(rs -> rs.getScopeIds().stream())
                        .toList();
                if (!isUnrestricted && !allowedUnitIds.contains(unitIdToAssign)) {
                    throw new UserMessageException(CommonErrorCode.ACCESS_DENIED.getCode(),
                            "SCHOOL_ADMIN chỉ được tạo tài khoản cho đơn vị của mình");
                }
            }
        } else if (unitIdToAssign != null) {
            // For non-SCHOOL_ADMIN roles, validate unitId is within allowed scopes
            if (!isUnrestricted) {
                List<Long> allowedUnitIds = allowedScopes.stream()
                        .flatMap(rs -> rs.getScopeIds().stream())
                        .toList();
                if (!allowedUnitIds.contains(unitIdToAssign)) {
                    throw new UserMessageException(CommonErrorCode.ACCESS_DENIED.getCode(),
                            "Không được tạo tài khoản cho đơn vị này");
                }
            }
        }

        // Step 5: Validate role-assignment permission: can current user assign requested role?
        boolean canAssignRole = roleAssignmentPermissionRepository
            .existsByCreatorRoleIdAndTargetRoleIdAndCanCreateAndStatusAndDeletedFlag(
                userScopes.getRoleId(),
                request.getRoleId(),
                1,
                1,
                0
            );

        if (!canAssignRole) {
            throw new UserMessageException(CommonErrorCode.ACCESS_DENIED.getCode(),
                "Bạn không được phép gán vai trò này");
        }

        // Step 6: Validate other duplicates
        String username = normalize(request.getUsername());
        validateUsernameDuplicate(username, null);

        // Step 7: Create user
        // ✅ NEW: User is now auth-only; profile data (fullName, email, phone, unit) 
        // will be managed via Staff entity after migration
        Role role = getRoleById(request.getRoleId());

        User user = new User();
        user.setUsername(username);
        user.setRole(role);
        user.setStatus(request.getStatus());
        user.setPasswordHash(encodePasswordFromClient(request.getPassword().trim()));
        user.setCreatedBy(SecurityUtils.getCurrentUsername());
        user.setDeletedFlag(0);

        // Step 8: Link Staff if provided
        if (request.getStaffId() != null) {
            Staff staff = staffRepository.findById(request.getStaffId())
                    .orElseThrow(() -> new UserMessageException(CommonErrorCode.STAFF_NOT_FOUND));
            user.setStaff(staff);
        }

        // NOTE: fullName, email, phone, unit are now profile data managed via Staff
        // This will be handled in Phase 2 with full API restructuring
        
        return userMapper.toDetailDto(userRepository.save(user));
    }

    // Cập nhật người dùng
    @Override
    @Transactional
    public UserDetailDto update(Long id, UserUpdateRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserMessageException(CommonErrorCode.USER_NOT_FOUND));

        // Enforce scope: validate user's unit is within allowed scopes before allowing update
        Long unitId = user.getUnitId();
        if (unitId != null) {
            ScopeFilterUtils.validateAccess(FEATURE, ActionType.EDIT, ScopeType.UNIT, unitId);
        }

        String username = normalize(request.getUsername());
        validateUsernameDuplicate(username, id);

        Role role = getRoleById(request.getRoleId());

        // If role is changing, validate current user can update to target role
        if (!user.getRole().getId().equals(request.getRoleId())) {
            UserScopes userScopes = SecurityContextUtils.getCurrentUserScopes()
                .orElseThrow(() -> new UserMessageException(CommonErrorCode.ACCESS_DENIED));

            boolean canUpdateRole = roleAssignmentPermissionRepository
                .existsByCreatorRoleIdAndTargetRoleIdAndCanUpdateAndStatusAndDeletedFlag(
                    userScopes.getRoleId(),
                    request.getRoleId(),
                    1,
                    1,
                    0
                );

            if (!canUpdateRole) {
            throw new UserMessageException(CommonErrorCode.ACCESS_DENIED.getCode(),
                "Bạn không được phép cập nhật sang vai trò này");
            }
        }

        applyCommonFields(user, request, role);
        applyPasswordIfPresent(user, request.getPassword());
        user.setUpdatedBy(SecurityUtils.getCurrentUsername());

        return userMapper.toDetailDto(userRepository.save(user));
    }

    // Xóa người dùng
    @Override
    @Transactional
    public void delete(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserMessageException(CommonErrorCode.USER_NOT_FOUND));

        // Enforce scope: validate user's unit is within allowed scopes before allowing delete
        Long unitId = user.getUnitId();
        if (unitId != null) {
            ScopeFilterUtils.validateAccess(FEATURE, ActionType.DELETE, ScopeType.UNIT, unitId);
        }

        // Xóa mềm: đánh dấu xóa 
        user.setDeletedFlag(1);
        user.setDeletedAt(LocalDateTime.now());
        user.setDeletedBy(SecurityUtils.getCurrentUsername());
        userRepository.save(user);
    }

    /**
     * Lấy Role theo ID, throw exception nếu không tìm thấy.
     */
    private Role getRoleById(Long roleId) {
        return roleRepository.findById(roleId)
                .orElseThrow(() -> new UserMessageException(CommonErrorCode.ROLE_NOT_FOUND));
    }

    /**
     * Validate username không trùng.
     * Khi update: excludeId cho phép user giữ nguyên username của chính nó.
     * 
     * @param username  tên đăng nhập cần check
     * @param excludeId ID user loại trừ (null khi create)
     */
    private void validateUsernameDuplicate(String username, Long excludeId) {
        boolean isDuplicate = excludeId == null
                ? userRepository.existsByUsername(username)
                : userRepository.existsByUsernameAndIdNot(username, excludeId);

        if (isDuplicate) {
            throw new UserMessageException(CommonErrorCode.USERNAME_ALREADY_EXISTS);
        }
    }

    /**
     * Áp dụng các trường chung từ request vào entity User.
     *  Profile fields (fullName, email, phone, unit) are now managed via Staff
     * User is authentication-only.
     */
    private void applyCommonFields(User user, UserUpdateRequest request, Role role) {
        user.setUsername(normalize(request.getUsername()));
        user.setRole(role);
        user.setStatus(request.getStatus());
        // Profile data (fullName, email, phone, unit) is no longer set on User
        // These are now managed via Staff entity
    }

    /**
     * Áp dụng thay đổi mật khẩu nếu có.
     * Xử lý convention: FE hash SHA256, BE hash SHA256 lần nữa.
     * 
     * @param user        entity User
     * @param rawPassword password từ request (có thể null)
     */
    private void applyPasswordIfPresent(User user, String rawPassword) {
        if (StringUtils.hasText(rawPassword)) {
            user.setPasswordHash(encodePasswordFromClient(rawPassword.trim()));
        }
    }

    /**
     * Mã hóa mật khẩu từ client.
     * Convention: FE gửi SHA256(password), BE apply SHA256 thêm lần nữa để bảo mật
     * thêm.
     * Note: Cách hash 2 lần này cần review lại trong tương lai.
     * 
     * @param clientPassword password từ client (đã SHA256 hash)
     * @return password hash 2 lần
     */
    private String encodePasswordFromClient(String clientPassword) {
        return PasswordUtils.sha256(clientPassword);
    }

    private UserFilterDto applyViewScopeToFilter(UserFilterDto filter) {
        UserFilterDto scopedFilter = new UserFilterDto();
        scopedFilter.setFullName(filter.getFullName());
        scopedFilter.setRoleId(filter.getRoleId());
        scopedFilter.setStatus(filter.getStatus());

        List<Long> requestedUnitIds = filter.getUnitId() == null ? List.of() : filter.getUnitId().stream()
                .filter(id -> id != null)
                .toList();

        List<ResolvedScope> allowedScopes = ScopeFilterUtils.getScopesForQuery(FEATURE, ActionType.VIEW);
        boolean isUnrestricted = allowedScopes.stream().anyMatch(ResolvedScope::isUnrestricted);
        if (isUnrestricted) {
            scopedFilter.setUnitId(requestedUnitIds.isEmpty() ? null : requestedUnitIds);
            return scopedFilter;
        }

        Set<Long> allowedUnitIds = allowedScopes.stream()
                .flatMap(rs -> rs.getScopeIds().stream())
                .filter(id -> id != null)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));

        if (requestedUnitIds.isEmpty()) {
            scopedFilter.setUnitId(allowedUnitIds.isEmpty() ? List.of(-1L) : new ArrayList<>(allowedUnitIds));
            return scopedFilter;
        }

        List<Long> intersectedUnitIds = requestedUnitIds.stream()
                .filter(allowedUnitIds::contains)
                .toList();
        scopedFilter.setUnitId(intersectedUnitIds.isEmpty() ? List.of(-1L) : intersectedUnitIds);
        return scopedFilter;
    }

    private byte[] exportUsersExcel(List<UserListItemDto> items) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Users");
            CellStyle titleStyle = createExportTitleStyle(workbook);
            CellStyle infoStyle = createExportInfoStyle(workbook);
            CellStyle headerStyle = createExportHeaderStyle(workbook);
            CellStyle bodyStyle = createExportBodyStyle(workbook);

            Row infoRow = sheet.createRow(0);
            infoRow.setHeightInPoints(18);
            var infoCell = infoRow.createCell(0);
            infoCell.setCellValue(buildExportInfoLine());
            infoCell.setCellStyle(infoStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 6));

            Row titleRow = sheet.createRow(1);
            titleRow.setHeightInPoints(26);
            var titleCell = titleRow.createCell(0);
            titleCell.setCellValue("DANH SÁCH NGƯỜI DÙNG");
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 6));

            Row headerRow = sheet.createRow(3);
            headerRow.setHeightInPoints(24);
            String[] headers = { "STT", "Tên đăng nhập", "Họ và tên", "Email", "Vai trò", "Đơn vị", "Trạng thái" };
            for (int i = 0; i < headers.length; i++) {
                var cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIndex = 4;
            int stt = 1;
            for (UserListItemDto item : items) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(stt++);
                row.createCell(1).setCellValue(item.getUsername() == null ? "" : item.getUsername());
                row.createCell(2).setCellValue(item.getFullName() == null ? "" : item.getFullName());
                row.createCell(3).setCellValue(item.getEmail() == null ? "" : item.getEmail());
                row.createCell(4).setCellValue(item.getRoleName() == null ? "" : item.getRoleName());
                row.createCell(5).setCellValue(item.getUnitName() == null ? "" : item.getUnitName());
                row.createCell(6).setCellValue(statusLabel(item.getStatus()));

                for (int col = 0; col <= 6; col++) {
                    row.getCell(col).setCellStyle(bodyStyle);
                }
            }

            sheet.setColumnWidth(0, 8 * 256);
            sheet.setColumnWidth(1, 26 * 256);
            sheet.setColumnWidth(2, 30 * 256);
            sheet.setColumnWidth(3, 34 * 256);
            sheet.setColumnWidth(4, 22 * 256);
            sheet.setColumnWidth(5, 28 * 256);
            sheet.setColumnWidth(6, 16 * 256);

            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException ex) {
            throw new UserMessageException("Không thể tạo file Excel danh sách người dùng");
        }
    }

    private byte[] exportUsersPdf(List<UserListItemDto> items) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4.rotate(), 24, 24, 20, 20);
            PdfWriter.getInstance(document, outputStream);
            document.open();

            com.lowagie.text.Font titleFont = createPdfFont(16, com.lowagie.text.Font.BOLD);
            com.lowagie.text.Font headerFont = createPdfFont(10, com.lowagie.text.Font.BOLD);
            com.lowagie.text.Font bodyFont = createPdfFont(10, com.lowagie.text.Font.NORMAL);
            com.lowagie.text.Font infoFont = createPdfFont(10, com.lowagie.text.Font.ITALIC);

                Paragraph exportInfo = new Paragraph(buildExportInfoLine(), infoFont);
            exportInfo.setAlignment(Element.ALIGN_RIGHT);
            exportInfo.setSpacingAfter(6f);
                document.add(exportInfo);

            Paragraph title = new Paragraph("DANH SÁCH NGƯỜI DÙNG", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(12f);
            document.add(title);

            PdfPTable table = new PdfPTable(new float[] { 0.8f, 2.0f, 2.5f, 2.8f, 2.0f, 2.3f, 1.4f });
            table.setWidthPercentage(100);

            addPdfHeaderCell(table, "STT", headerFont);
            addPdfHeaderCell(table, "Tên đăng nhập", headerFont);
            addPdfHeaderCell(table, "Họ và tên", headerFont);
            addPdfHeaderCell(table, "Email", headerFont);
            addPdfHeaderCell(table, "Vai trò", headerFont);
            addPdfHeaderCell(table, "Đơn vị", headerFont);
            addPdfHeaderCell(table, "Trạng thái", headerFont);

            int stt = 1;
            for (UserListItemDto item : items) {
                addPdfBodyCell(table, String.valueOf(stt++), bodyFont, Element.ALIGN_CENTER);
                addPdfBodyCell(table, item.getUsername(), bodyFont, Element.ALIGN_LEFT);
                addPdfBodyCell(table, item.getFullName(), bodyFont, Element.ALIGN_LEFT);
                addPdfBodyCell(table, item.getEmail(), bodyFont, Element.ALIGN_LEFT);
                addPdfBodyCell(table, item.getRoleName(), bodyFont, Element.ALIGN_LEFT);
                addPdfBodyCell(table, item.getUnitName(), bodyFont, Element.ALIGN_LEFT);
                addPdfBodyCell(table, statusLabel(item.getStatus()), bodyFont, Element.ALIGN_CENTER);
            }

            document.add(table);
            document.close();
            return outputStream.toByteArray();
        } catch (DocumentException | IOException ex) {
            throw new UserMessageException("Không thể tạo file PDF danh sách người dùng");
        }
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

    private CellStyle createExportHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setFillForegroundColor((short) 41);
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);

        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontName(EXPORT_FONT_NAME);
        style.setFont(font);
        return style;
    }

    private CellStyle createExportTitleStyle(Workbook workbook) {
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
            // Fallback to font factory below.
        }

        return com.lowagie.text.FontFactory.getFont(EXPORT_FONT_NAME, BaseFont.IDENTITY_H, true, size, style);
    }

    private CellStyle createExportInfoStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.RIGHT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        Font font = workbook.createFont();
        font.setFontHeightInPoints((short) 10);
        font.setFontName(EXPORT_FONT_NAME);
        style.setFont(font);
        return style;
    }

    private CellStyle createExportBodyStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setVerticalAlignment(VerticalAlignment.TOP);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setWrapText(true);

        Font font = workbook.createFont();
        font.setFontName(EXPORT_FONT_NAME);
        style.setFont(font);
        return style;
    }

    private String statusLabel(Integer status) {
        return Integer.valueOf(1).equals(status) ? "Hoạt động" : "Không hoạt động";
    }

    private String buildExportInfoLine() {
        String exportTime = LocalDateTime.now().format(EXPORT_TIME_FORMATTER);
        String username = SecurityUtils.getCurrentUsername();
        return "Thời gian tải: " + exportTime + " | Người tải: " + username;
    }

    /**
     * Chuẩn hóa kích thước trang phân trang.
     */
    private int normalizePageSize(Integer pageSize) {
        return pageSize == null || pageSize <= 0 ? 10 : pageSize;
    }

    /**
     * Chuẩn hóa số trang hiện tại.
     */
    private int normalizePageNow(Integer pageNow) {
        return pageNow == null || pageNow <= 0 ? 1 : pageNow;
    }

    /**
     * Chuẩn hóa string: trim.
     */
    private String normalize(String value) {
        return value == null ? null : value.trim();
    }
}
