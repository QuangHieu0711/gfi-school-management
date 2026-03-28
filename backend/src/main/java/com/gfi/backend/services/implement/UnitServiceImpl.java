package com.gfi.backend.services.implement;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gfi.backend.controllers.exceptions.UserMessageException;
import com.gfi.backend.models.dtos.common.PageRequestDto;
import com.gfi.backend.models.dtos.common.PageResponseDto;
import com.gfi.backend.models.dtos.unit.UnitCreateRequest;
import com.gfi.backend.models.dtos.unit.UnitFilterDto;
import com.gfi.backend.models.dtos.unit.UnitItemDto;
import com.gfi.backend.models.dtos.unit.UnitUpdateRequest;
import com.gfi.backend.models.entities.Unit;
import com.gfi.backend.models.global.CommonErrorCode;
import com.gfi.backend.repositories.UnitRepository;
import com.gfi.backend.repositories.UserRepository;
import com.gfi.backend.services.interfaces.UnitService;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UnitServiceImpl implements UnitService {

    private final UnitRepository unitRepository;
    private final UserRepository userRepository;

    @Override
    public PageResponseDto<UnitItemDto, UnitFilterDto> search(PageRequestDto<UnitFilterDto> request) {
        UnitFilterDto filter = request.getFilter() == null ? new UnitFilterDto() : request.getFilter();
        int pageSize = normalizePageSize(request.getPageSize());
        int pageNow = normalizePageNow(request.getPageNow());
        Pageable pageable = PageRequest.of(pageNow - 1, pageSize);

        Page<Unit> page = unitRepository.findAll(buildSpecification(filter), pageable);
        List<UnitItemDto> items = page.getContent().stream()
                .map(this::toDto)
                .toList();

        return PageResponseDto.<UnitItemDto, UnitFilterDto>builder()
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
    public UnitItemDto create(UnitCreateRequest request) {
        String code = normalize(request.getCode());
        if (unitRepository.existsByCode(code)) {
            throw new UserMessageException(CommonErrorCode.UNIT_CODE_ALREADY_EXISTS);
        }

        Unit unit = new Unit();
        unit.setCode(code);
        unit.setName(normalize(request.getName()));
        unit.setAddress(normalizeNullable(request.getAddress()));
        unit.setPhone(normalizeNullable(request.getPhone()));
        unit.setEmail(normalizeNullable(request.getEmail()));
        unit.setStatus(request.getStatus());
        unit.setCreatedBy(getCurrentUsername());
        return toDto(unitRepository.save(unit));
    }

    @Override
    @Transactional
    public UnitItemDto update(Long id, UnitUpdateRequest request) {
        Unit unit = unitRepository.findById(id)
                .orElseThrow(() -> new UserMessageException(CommonErrorCode.UNIT_NOT_FOUND));

        String code = normalize(request.getCode());
        unitRepository.findByCode(code)
                .filter(found -> !found.getId().equals(id))
                .ifPresent(found -> {
                    throw new UserMessageException(CommonErrorCode.UNIT_CODE_ALREADY_EXISTS);
                });

        unit.setCode(code);
        unit.setName(normalize(request.getName()));
        unit.setAddress(normalizeNullable(request.getAddress()));
        unit.setPhone(normalizeNullable(request.getPhone()));
        unit.setEmail(normalizeNullable(request.getEmail()));
        unit.setStatus(request.getStatus());
        unit.setUpdatedBy(getCurrentUsername());
        return toDto(unitRepository.save(unit));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Unit unit = unitRepository.findById(id)
                .orElseThrow(() -> new UserMessageException(CommonErrorCode.UNIT_NOT_FOUND));

        if (userRepository.countByUnitId(id) > 0) {
            throw new UserMessageException(CommonErrorCode.UNIT_IN_USE);
        }

        unitRepository.delete(unit);
    }

    private Specification<Unit> buildSpecification(UnitFilterDto filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (hasText(filter.getUnitName())) {
                String keyword = "%" + filter.getUnitName().trim().toLowerCase() + "%";
                predicates.add(cb.like(cb.lower(root.get("name")), keyword));
            }
            if (filter.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), filter.getStatus()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private UnitItemDto toDto(Unit unit) {
        return UnitItemDto.builder()
                .id(unit.getId())
                .code(unit.getCode())
                .name(unit.getName())
                .address(unit.getAddress())
                .phone(unit.getPhone())
                .email(unit.getEmail())
                .status(unit.getStatus())
                .createdAt(unit.getCreatedAt())
                .createdBy(unit.getCreatedBy())
                .updatedAt(unit.getUpdatedAt())
                .updatedBy(unit.getUpdatedBy())
                .build();
    }

    private int normalizePageSize(Integer pageSize) {
        return pageSize == null || pageSize <= 0 ? 10 : pageSize;
    }

    private int normalizePageNow(Integer pageNow) {
        return pageNow == null || pageNow <= 0 ? 1 : pageNow;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }

    private String normalizeNullable(String value) {
        return hasText(value) ? value.trim() : null;
    }

    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || "anonymousUser".equals(authentication.getName())) {
            return "SYSTEM";
        }
        return authentication.getName();
    }
}
