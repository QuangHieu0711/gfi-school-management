package com.gfi.backend.services.implement;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gfi.backend.controllers.exceptions.UserMessageException;
import com.gfi.backend.models.dtos.common.PageRequestDto;
import com.gfi.backend.models.dtos.common.PageResponseDto;
import com.gfi.backend.models.dtos.user.UserCreateRequest;
import com.gfi.backend.models.dtos.user.UserFilterDto;
import com.gfi.backend.models.dtos.user.UserItemDto;
import com.gfi.backend.models.dtos.user.UserUpdateRequest;
import com.gfi.backend.models.entities.Role;
import com.gfi.backend.models.entities.Unit;
import com.gfi.backend.models.entities.User;
import com.gfi.backend.models.global.CommonErrorCode;
import com.gfi.backend.repositories.RoleRepository;
import com.gfi.backend.repositories.UnitRepository;
import com.gfi.backend.repositories.UserRepository;
import com.gfi.backend.services.interfaces.UserService;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UnitRepository unitRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public PageResponseDto<UserItemDto, UserFilterDto> search(PageRequestDto<UserFilterDto> request) {
        UserFilterDto filter = request.getFilter() == null ? new UserFilterDto() : request.getFilter();
        int pageSize = normalizePageSize(request.getPageSize());
        int pageNow = normalizePageNow(request.getPageNow());
        Pageable pageable = PageRequest.of(pageNow - 1, pageSize);

        Page<User> page = userRepository.findAll(buildSpecification(filter), pageable);
        List<UserItemDto> items = page.getContent().stream()
                .map(this::toDto)
                .toList();

        return PageResponseDto.<UserItemDto, UserFilterDto>builder()
                .pageSize(pageSize)
                .pageNow(pageNow)
                .filter(filter)
                .pageTotal(page.getTotalPages())
                .recordTotal(page.getTotalElements())
                .items(items)
                .build();
    }

    @Override
    public UserItemDto getById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserMessageException(CommonErrorCode.USER_NOT_FOUND));
        return toDto(user);
    }

    @Override
    @Transactional
    public UserItemDto create(UserCreateRequest request) {
        String username = normalize(request.getUsername());
        if (userRepository.existsByUsername(username)) {
            throw new UserMessageException(CommonErrorCode.USERNAME_ALREADY_EXISTS);
        }

        Role role = roleRepository.findById(request.getRoleId())
                .orElseThrow(() -> new UserMessageException(CommonErrorCode.ROLE_NOT_FOUND));
        Unit unit = unitRepository.findById(request.getUnitId())
                .orElseThrow(() -> new UserMessageException(CommonErrorCode.UNIT_NOT_FOUND));

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(request.getPassword().trim()));
        user.setFullName(normalize(request.getFullName()));
        user.setEmail(normalizeNullable(request.getEmail()));
        user.setRole(role);
        user.setUnit(unit);
        user.setStatus(request.getStatus());
        user.setCreatedBy(getCurrentUsername());

        return toDto(userRepository.save(user));
    }

    @Override
    @Transactional
    public UserItemDto update(Long id, UserUpdateRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserMessageException(CommonErrorCode.USER_NOT_FOUND));

        String username = normalize(request.getUsername());
        userRepository.findByUsername(username)
                .filter(found -> !found.getId().equals(id))
                .ifPresent(found -> {
                    throw new UserMessageException(CommonErrorCode.USERNAME_ALREADY_EXISTS);
                });

        Role role = roleRepository.findById(request.getRoleId())
                .orElseThrow(() -> new UserMessageException(CommonErrorCode.ROLE_NOT_FOUND));
        Unit unit = unitRepository.findById(request.getUnitId())
                .orElseThrow(() -> new UserMessageException(CommonErrorCode.UNIT_NOT_FOUND));

        user.setUsername(username);
        user.setFullName(normalize(request.getFullName()));
        user.setEmail(normalizeNullable(request.getEmail()));
        user.setRole(role);
        user.setUnit(unit);
        user.setStatus(request.getStatus());
        user.setUpdatedBy(getCurrentUsername());
        if (hasText(request.getPassword())) {
            user.setPassword(passwordEncoder.encode(request.getPassword().trim()));
        }

        return toDto(userRepository.save(user));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserMessageException(CommonErrorCode.USER_NOT_FOUND));
        userRepository.delete(user);
    }

    private Specification<User> buildSpecification(UserFilterDto filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            Join<Object, Object> roleJoin = root.join("role", JoinType.LEFT);
            Join<Object, Object> unitJoin = root.join("unit", JoinType.LEFT);

            if (hasText(filter.getFullName())) {
                String keyword = "%" + filter.getFullName().trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("username")), keyword),
                        cb.like(cb.lower(root.get("fullName")), keyword)));
            }
            if (filter.getRoleId() != null) {
                predicates.add(cb.equal(roleJoin.get("id"), filter.getRoleId()));
            }
            if (filter.getUnitId() != null) {
                predicates.add(cb.equal(unitJoin.get("id"), filter.getUnitId()));
            }
            if (filter.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), filter.getStatus()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
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

    private UserItemDto toDto(User user) {
        return UserItemDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .status(user.getStatus())
                .roleId(user.getRole() == null ? null : user.getRole().getId())
                .roleName(user.getRole() == null ? null : user.getRole().getRoleName())
                .unitId(user.getUnit() == null ? null : user.getUnit().getId())
                .unitName(user.getUnit() == null ? null : user.getUnit().getName())
                .build();
    }

    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || "anonymousUser".equals(authentication.getName())) {
            return "SYSTEM";
        }
        return authentication.getName();
    }
}
