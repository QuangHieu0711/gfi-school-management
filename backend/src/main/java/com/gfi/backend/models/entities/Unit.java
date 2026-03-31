package com.gfi.backend.models.entities;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "units")
@Data
public class Unit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String code;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(length = 255)
    private String address;

    @Column(length = 50)
    private String phone;

    @Column(length = 100)
    private String email;

    @Column
    private Integer status;

    @Column
    private LocalDateTime createdAt;

    @Column(length = 255)
    private String createdBy;

    @Column
    private LocalDateTime updatedAt;

    @Column(length = 255)
    private String updatedBy;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = 1;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
