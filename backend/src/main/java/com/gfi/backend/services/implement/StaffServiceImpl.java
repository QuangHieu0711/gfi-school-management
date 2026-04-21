package com.gfi.backend.services.implement;

import com.gfi.backend.models.dtos.staff.*;
import com.gfi.backend.models.dtos.common.PageRequestDto;
import com.gfi.backend.models.dtos.common.PageResponseDto;
import com.gfi.backend.models.entities.Staff;
import com.gfi.backend.models.entities.StaffAddress;
import com.gfi.backend.models.entities.StaffFamilyMember;
import com.gfi.backend.models.entities.Unit;
import com.gfi.backend.models.entities.GradeLevel;
import com.gfi.backend.models.global.CommonErrorCode;
import com.gfi.backend.controllers.exceptions.UserMessageException;
import com.gfi.backend.repositories.StaffAddressRepository;
import com.gfi.backend.repositories.StaffFamilyMemberRepository;
import com.gfi.backend.repositories.StaffRepository;
import com.gfi.backend.repositories.UnitRepository;
import com.gfi.backend.repositories.UserRepository;
import com.gfi.backend.models.enums.ActionType;
import com.gfi.backend.models.enums.ScopeType;
import com.gfi.backend.models.security.ResolvedScope;
import com.gfi.backend.services.FileStorageService;
import com.gfi.backend.services.interfaces.StaffService;
import com.gfi.backend.utils.PageableUtils;
import com.gfi.backend.utils.ScopeFilterUtils;
import com.gfi.backend.utils.SecurityContextUtils;
import com.gfi.backend.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    private final StaffRepository staffRepository;
    private final UnitRepository unitRepository;
    private final com.gfi.backend.repositories.GradeLevelRepository gradeLevelRepository;
    private final StaffAddressRepository staffAddressRepository;
    private final StaffFamilyMemberRepository staffFamilyMemberRepository;
    private final FileStorageService fileStorageService;
    private final UserRepository userRepository;

    private static final String FEATURE = "STAFF_PROFILE";

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
        int pageSize = normalizePageSize(request.getPageSize());
        int pageNow = normalizePageNow(request.getPageNow());
        Pageable pageable = PageableUtils.newestFirst(pageNow, pageSize);

        Page<Staff> page = staffRepository.findAll(buildSpecification(filter, currentStaffId), pageable);
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
        validateSelfAccess(id, ActionType.VIEW);
        Staff staff = findStaff(id);
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
        validateSelfAccess(id, ActionType.EDIT);
        Staff staff = findStaff(id);
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
        validateSelfAccess(id, ActionType.DELETE);
        Staff staff = findStaff(id);
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
    private Specification<Staff> buildSpecification(StaffFilterDto filter, Long forcedStaffId) {
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

            // Always filter deleted flag
            predicates.add(cb.equal(root.get("deletedFlag"), 0));

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private void validateSelfAccess(Long targetStaffId, ActionType actionType) {
        Long currentStaffId = resolveCurrentStaffIdForSelfScope(actionType);
        if (currentStaffId != null && !currentStaffId.equals(targetStaffId)) {
            throw new UserMessageException(CommonErrorCode.ACCESS_DENIED);
        }
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
}
