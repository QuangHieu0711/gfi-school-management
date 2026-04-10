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
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "student_addresses")
@Getter
@Setter
public class StudentAddress extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false, foreignKey = @ForeignKey(name = "fk_student_addresses_student"))
    private Student student;

    @Column(nullable = false, length = 30)
    private String addressType;

    @Column(length = 255)
    private String provinceName;

    @Column(length = 255)
    private String wardName;

    @Column(length = 255)
    private String hamletName;

    @Column(length = 500)
    private String detailAddress;
}
