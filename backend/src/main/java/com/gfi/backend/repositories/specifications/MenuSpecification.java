package com.gfi.backend.repositories.specifications;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.gfi.backend.models.dtos.menu.MenuFilterDto;
import com.gfi.backend.models.entities.Menu;

import jakarta.persistence.criteria.Predicate;

/**
 * Specification builder cho Menu query.
 * Tách biệt logic truy vấn từ service.
 * Đóng gói các tiêu chí lọc menu: từ khóa tìm kiếm, trạng thái xóa.
 */
@Component
public class MenuSpecification {

    /**
     * Xây dựng Specification từ filter conditions.
     * Lọc menu theo từ khóa (code hoặc name).
     * Luôn loại bỏ menu đã xóa (deletedFlag = 0).
     * 
     * @param filter điều kiện lọc
     * @return Specification<Menu> dùng cho repository query
     */
    public Specification<Menu> buildSpecification(MenuFilterDto filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Luôn lọc các menu chưa xóa
            predicates.add(cb.equal(root.get("deletedFlag"), 0));

            // Tìm kiếm với từ khóa là mã hoặc tên menu
            if (StringUtils.hasText(filter.getMenu())) {
                String keyword = "%" + filter.getMenu().trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("code")), keyword),
                        cb.like(cb.lower(root.get("name")), keyword)));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Xây dựng Specification để lọc chỉ những menu chưa xóa.
     * Dùng cho getOptions() và các query khác cần menu active.
     * 
     * @return Specification<Menu> lọc deletedFlag = 0
     */
    public Specification<Menu> buildActiveOnlySpecification() {
        return (root, query, cb) -> cb.equal(root.get("deletedFlag"), 0);
    }
}
