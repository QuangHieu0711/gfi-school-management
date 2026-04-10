package com.gfi.backend.models.mappers;

import com.gfi.backend.models.dtos.unit.UnitDetailDto;
import com.gfi.backend.models.dtos.unit.UnitListItemDto;
import com.gfi.backend.models.entities.Unit;
import org.springframework.stereotype.Component;

/**
 * Mapper chuyển Unit entity sang các DTO.
 * Tách biệt mapping logic khỏi service layer.
 */
@Component
public class UnitMapper {

    /**
     * Chuyển Unit entity sang UnitListItemDto (cho list/search).
     * Chỉ chứa những field cần thiết để tránh over-fetching.
     */
    public UnitListItemDto toListItemDto(Unit unit) {
        return UnitListItemDto.builder()
                .id(unit.getId())
                .code(unit.getCode())
                .name(unit.getName())
                .address(unit.getAddress())
                .status(unit.getStatus())
                .build();
    }

    /**
     * Chuyển Unit entity sang UnitDetailDto (cho detail/create/update).
     * Chứa đầy đủ thông tin.
     */
    public UnitDetailDto toDetailDto(Unit unit) {
        return UnitDetailDto.builder()
                .id(unit.getId())
                .code(unit.getCode())
                .name(unit.getName())
                .address(unit.getAddress())
                .phone(unit.getPhone())
                .email(unit.getEmail())
                .status(unit.getStatus())
                .build();
    }
}
