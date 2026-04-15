package com.gfi.backend.models.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;

@Entity
@Table(name = "staff_job_histories")
@Getter
@Setter
public class StaffJobHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "staff_id", nullable = false, foreignKey = @ForeignKey(name = "fk_staff_job_histories_staff"))
    private Staff staff;

    @Column(nullable = false)
    private LocalDate fromDate;

    @Column
    private LocalDate toDate;

    @Column
    private Long unitId;

    @Column
    private Long departmentId;

    @Column
    private Long workingPositionId;

    @Column
    private Long titleId;

    @Column
    private Long employmentTypeId;

    @Column(length = 100)
    private String decisionNo;

    @Column(columnDefinition = "TEXT")
    private String note;
}
