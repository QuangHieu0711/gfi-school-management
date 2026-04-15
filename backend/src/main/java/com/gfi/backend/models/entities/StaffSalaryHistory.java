package com.gfi.backend.models.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
import java.math.BigDecimal;

@Entity
@Table(name = "staff_salary_histories")
@Getter
@Setter
public class StaffSalaryHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "staff_id", nullable = false, foreignKey = @ForeignKey(name = "fk_staff_salary_histories_staff"))
    private Staff staff;

    @Column(nullable = false)
    private LocalDate effectiveDate;

    @Column
    private Long salaryRankId;

    @Column(length = 50)
    private String salaryCode;

    @Column
    private Integer salaryStep;

    @Column(precision = 5, scale = 2)
    private BigDecimal salaryCoefficient;

    @Column(precision = 5, scale = 2)
    private BigDecimal vuotKhuongPct;

    @Column(precision = 5, scale = 2)
    private BigDecimal seniorityPct;

    @Column(precision = 5, scale = 2)
    private BigDecimal preferentialPct;

    @Column(precision = 5, scale = 2)
    private BigDecimal responsibilityPct;

    @Column(precision = 5, scale = 2)
    private BigDecimal positionAllowancePct;

    @Column(precision = 5, scale = 2)
    private BigDecimal classHeadAllowancePct;

    @Column(precision = 5, scale = 2)
    private BigDecimal homeroomAllowancePct;

    @Column(precision = 5, scale = 2)
    private BigDecimal hazardousAllowancePct;

    @Column(precision = 10, scale = 2)
    private BigDecimal regionAllowance;

    @Column(precision = 10, scale = 2)
    private BigDecimal longTermAllowance;

    @Column(precision = 10, scale = 2)
    private BigDecimal attractionAllowance;

    @Column
    private Boolean isOnSalaryInsurance;

    @Column(columnDefinition = "TEXT")
    private String note;
}
