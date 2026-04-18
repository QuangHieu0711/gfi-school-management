package com.gfi.backend.models.entities;

import java.time.LocalDate;

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
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "week_configs", uniqueConstraints = {
        @UniqueConstraint(name = "uk_week_configs_year_semester_week", columnNames = { "school_year_id", "semester_id",
                "week_number" })
})
@Getter
@Setter
public class WeekConfig extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "school_year_id", nullable = false, foreignKey = @ForeignKey(name = "fk_week_configs_school_years"))
    private SchoolYear schoolYear;

    @ManyToOne
    @JoinColumn(name = "semester_id", nullable = false, foreignKey = @ForeignKey(name = "fk_week_configs_semesters"))
    private Semester semester;

    @Column(nullable = false)
    private Integer weekNumber;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;
}
