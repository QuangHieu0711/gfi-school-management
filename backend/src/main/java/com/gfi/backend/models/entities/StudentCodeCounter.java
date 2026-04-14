package com.gfi.backend.models.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Bảng đếm để sinh mã học sinh tự động.
 * 
 * Format mã: HS-{MA_DON_VI}-{NAM}-{STT}
 * Ví dụ: HS-TH01-2025-0001
 * 
 * Ràng buộc unique trên (unit_id, year) để đảm bảo mỗi đơn vị mỗi năm có 1 dòng counter
 */
@Entity
@Table(name = "student_code_counters", uniqueConstraints = {
        @UniqueConstraint(name = "uk_student_code_counters_unit_year", columnNames = { "unit_id", "year" })
})
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class StudentCodeCounter extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "unit_id", nullable = false, foreignKey = @ForeignKey(name = "fk_student_code_counters_units"))
    private Unit unit;

    @Column(nullable = false)
    private Integer year;

    @Column(nullable = false)
    private Long lastNumber;
}
