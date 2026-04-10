package com.gfi.backend.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.gfi.backend.models.entities.Menu;

@Repository
public interface MenuRepository extends JpaRepository<Menu, Long>, JpaSpecificationExecutor<Menu> {
    
    Optional<Menu> findByCode(String code);
    
    boolean existsByCode(String code);
    
    long countByParentMenuId(Long parentMenuId);

    /**
     * Tìm menu theo mã, chỉ trả về menu chưa xóa (deletedFlag = 0)
     */
    @Query("SELECT m FROM Menu m WHERE m.code = :code AND m.deletedFlag = 0")
    Optional<Menu> findByCodeAndDeletedFlagZero(@Param("code") String code);

    /**
     * Đếm số menu con chưa xóa của một menu cha
     */
    @Query("SELECT COUNT(m) FROM Menu m WHERE m.parentMenu.id = :parentId AND m.deletedFlag = 0")
    long countActiveByParentMenuId(@Param("parentId") Long parentId);
}
