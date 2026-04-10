package com.gfi.backend.services.implement;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.gfi.backend.controllers.exceptions.UserMessageException;
import com.gfi.backend.models.dtos.common.LookupItemDto;
import com.gfi.backend.models.dtos.common.PageRequestDto;
import com.gfi.backend.models.dtos.common.PageResponseDto;
import com.gfi.backend.models.dtos.unit.UnitCreateRequest;
import com.gfi.backend.models.dtos.unit.UnitDetailDto;
import com.gfi.backend.models.dtos.unit.UnitFilterDto;
import com.gfi.backend.models.dtos.unit.UnitListItemDto;
import com.gfi.backend.models.dtos.unit.UnitUpdateRequest;
import com.gfi.backend.models.entities.Unit;
import com.gfi.backend.models.global.CommonErrorCode;
import com.gfi.backend.models.mappers.UnitMapper;
import com.gfi.backend.repositories.ClassroomRepository;
import com.gfi.backend.repositories.UnitRepository;
import com.gfi.backend.repositories.UserRepository;
import com.gfi.backend.repositories.specifications.UnitSpecification;
import com.gfi.backend.services.interfaces.UnitService;
import com.gfi.backend.utils.PageableUtils;
import com.gfi.backend.utils.SecurityUtils;

import lombok.RequiredArgsConstructor;

/**
 * Service xử lý logic quản lý đơn vị.
 * 
 * Trách nhiệm tách biệt:
 * - Logic query: UnitSpecification
 * - Logic mapping: UnitMapper
 * - Validate & load relations: private helpers
 * - Security: SecurityUtils
 */
@Service
@RequiredArgsConstructor
public class UnitServiceImpl implements UnitService {

    private final UnitRepository unitRepository;
    private final UserRepository userRepository;
    private final ClassroomRepository classroomRepository;
    private final UnitSpecification unitSpecification;
    private final UnitMapper unitMapper;

    // Tìm kiếm và phân trang units với filter
    @Override
    @Transactional(readOnly = true)
    public PageResponseDto<UnitListItemDto, UnitFilterDto> search(PageRequestDto<UnitFilterDto> request) {
        UnitFilterDto filter = request.getFilter() == null ? new UnitFilterDto() : request.getFilter();
        int pageSize = normalizePageSize(request.getPageSize());
        int pageNow = normalizePageNow(request.getPageNow());
        Pageable pageable = PageableUtils.newestFirst(pageNow, pageSize);

        Page<Unit> page = unitRepository.findAll(unitSpecification.buildSpecification(filter), pageable);
        List<UnitListItemDto> items = page.getContent().stream()
                .map(unitMapper::toListItemDto)
                .toList();

        return PageResponseDto.<UnitListItemDto, UnitFilterDto>builder()
                .pageSize(pageSize)
                .pageNow(pageNow)
                .filter(filter)
                .pageTotal(page.getTotalPages())
                .recordTotal(page.getTotalElements())
                .items(items)
                .build();
    }

    // Lấy danh sách đơn vị cho dropdown/combobox
    @Override
    @Transactional(readOnly = true)
    public List<LookupItemDto> getOptions() {
        return unitRepository.findAll(Sort.by(Sort.Direction.ASC, "name")).stream()
                .map(unit -> LookupItemDto.builder()
                        .id(unit.getId())
                        .name(unit.getName())
                        .build())
                .toList();
    }

    // Chi tiết đơn vị theo ID
    @Override
    @Transactional(readOnly = true)
    public UnitDetailDto getById(Long id) {
        Unit unit = unitRepository.findById(id)
                .orElseThrow(() -> new UserMessageException(CommonErrorCode.UNIT_NOT_FOUND));
        return unitMapper.toDetailDto(unit);
    }

    // Thêm mới đơn vị
    @Override
    @Transactional
    public UnitDetailDto create(UnitCreateRequest request) {
        String code = normalize(request.getCode());
        validateCodeDuplicate(code, null);

        Unit unit = new Unit();
        unit.setCode(code);
        unit.setName(normalize(request.getName()));
        unit.setAddress(normalizeNullable(request.getAddress()));
        unit.setPhone(normalizeNullable(request.getPhone()));
        unit.setEmail(normalizeNullable(request.getEmail()));
        unit.setStatus(request.getStatus());
        unit.setCreatedBy(SecurityUtils.getCurrentUsername());

        return unitMapper.toDetailDto(unitRepository.save(unit));
    }

    // Cập nhật đơn vị
    @Override
    @Transactional
    public UnitDetailDto update(Long id, UnitUpdateRequest request) {
        Unit unit = unitRepository.findById(id)
                .orElseThrow(() -> new UserMessageException(CommonErrorCode.UNIT_NOT_FOUND));

        String code = normalize(request.getCode());
        validateCodeDuplicate(code, id);

        unit.setCode(code);
        unit.setName(normalize(request.getName()));
        unit.setAddress(normalizeNullable(request.getAddress()));
        unit.setPhone(normalizeNullable(request.getPhone()));
        unit.setEmail(normalizeNullable(request.getEmail()));
        unit.setStatus(request.getStatus());
        unit.setUpdatedBy(SecurityUtils.getCurrentUsername());

        return unitMapper.toDetailDto(unitRepository.save(unit));
    }

    // ==================== XÓA ====================
    @Override
    @Transactional
    public void delete(Long id) {
        Unit unit = unitRepository.findById(id)
                .orElseThrow(() -> new UserMessageException(CommonErrorCode.UNIT_NOT_FOUND));

        // Kiểm tra unit có được sử dụng không
        if (userRepository.countByUnitId(id) > 0) {
            throw new UserMessageException(CommonErrorCode.UNIT_IN_USE);
        }
        if (classroomRepository.countByUnitId(id) > 0) {
            throw new UserMessageException(CommonErrorCode.UNIT_IN_USE);
        }

        // Xóa mềm: đánh dấu xóa
        unit.setDeletedFlag(1);
        unit.setDeletedAt(LocalDateTime.now());
        unit.setDeletedBy(SecurityUtils.getCurrentUsername());
        unitRepository.save(unit);
    }

    /**
     * Validate code không trùng.
     * Khi update: excludeId cho phép unit giữ nguyên code của chính nó.
     * 
     * @param code mã unit cần check
     * @param excludeId ID unit loại trừ (null khi create)
     */
    private void validateCodeDuplicate(String code, Long excludeId) {
        boolean isDuplicate = excludeId == null
                ? unitRepository.existsByCode(code)
                : unitRepository.existsByCodeAndIdNot(code, excludeId);
        
        if (isDuplicate) {
            throw new UserMessageException(CommonErrorCode.UNIT_CODE_ALREADY_EXISTS);
        }
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

    /**
     * Chuẩn hóa string nullable: return null nếu rỗng hoặc whitespace.
     */
    private String normalizeNullable(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
