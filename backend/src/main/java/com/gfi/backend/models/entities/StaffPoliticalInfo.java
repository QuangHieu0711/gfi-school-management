package com.gfi.backend.models.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;

@Entity
@Table(name = "staff_political_infos")
@Getter
@Setter
public class StaffPoliticalInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "staff_id", nullable = false, unique = true, foreignKey = @ForeignKey(name = "fk_staff_political_infos_staff"))
    private Staff staff;

    @Column
    private Long maritalStatusId;

    @Column(length = 255)
    private String familyBackground;

    @Column
    private LocalDate partyJoinDate;

    @Column
    private LocalDate partyOfficialDate;

    @Column(length = 255)
    private String partyJoinPlace;

    @Column
    private Boolean isPartyMember;

    @Column
    private Boolean isUnionMember;

    @Column
    private LocalDate unionJoinDate;

    @Column
    private Boolean isYouthUnionMember;

    @Column
    private LocalDate youthUnionJoinDate;

    @Column(columnDefinition = "TEXT")
    private String note;
}
