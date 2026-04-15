package com.gfi.backend.models.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
import java.math.BigDecimal;

@Entity
@Table(name = "staff_job_infos")
@Getter
@Setter
public class StaffJobInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "staff_id", nullable = false, unique = true, foreignKey = @ForeignKey(name = "fk_staff_job_infos_staff"))
    private Staff staff;

    @Column
    private Long mainSubjectId;

    @Column
    private Long teachingLevelId;

    @Column
    private Long workingPositionId;

    @Column
    private Long departmentId;

    @Column
    private Long titleId;

    @Column
    private Long roleGroupId;

    @Column
    private Long employmentTypeId;

    @Column
    private LocalDate recruitmentDate;

    @Column
    private LocalDate schoolJoinDate;

    @Column
    private LocalDate officialDate;

    @Column
    private Integer yearlyTeachingSessions;

    @Column(precision = 5, scale = 2)
    private BigDecimal dailyTeachingSessions;

    @Column(length = 255)
    private String recruitmentAgency;

    @Column(length = 255)
    private String appointmentBy;

    @Column(length = 255)
    private String concurrentTask;

    @Column
    private Boolean isEthnicLanguageCert;

    @Column
    private Boolean isPartyTraining;

    @Column
    private Boolean isRetired;
}
