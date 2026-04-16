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

@Entity
@Table(name = "staff_code_counters", uniqueConstraints = {
        @UniqueConstraint(name = "uk_staff_code_counters_unit_year", columnNames = { "unit_id", "year" })
})
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class StaffCodeCounter extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "unit_id", nullable = false, foreignKey = @ForeignKey(name = "fk_staff_code_counters_units"))
    private Unit unit;

    @Column(nullable = false)
    private Integer year;

    @Column(nullable = false)
    private Long lastNumber;
}
