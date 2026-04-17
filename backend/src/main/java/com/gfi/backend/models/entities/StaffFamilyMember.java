package com.gfi.backend.models.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "staff_family_members")
@Getter
@Setter
public class StaffFamilyMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "staff_id", nullable = false, foreignKey = @ForeignKey(name = "fk_staff_family_members_staff"))
    private Staff staff;

    @Column(nullable = false, length = 30)
    private String relationType;

    @Column(nullable = false, length = 255)
    private String fullName;

    @Column
    private Integer birthYear;

    @Column(length = 255)
    private String placeOfBirth;

    @Column(length = 255)
    private String hometown;

    @Column(length = 255)
    private String occupation;

    @Column(length = 50)
    private String phone;

    @Column(length = 255)
    private String workplace;

    @Column(length = 500)
    private String address;

    @Column(columnDefinition = "TEXT")
    private String note;
}
