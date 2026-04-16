package com.gfi.backend.models.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "roles")
public class Role extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, unique = true, length = 100)
    private String roleName;

    @Column(length = 255)
    private String description;

    @Column(nullable = false)
    private Integer status;

    // Role-based configuration flags (DB-driven, no hard-code)
    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean requiresStaffProfile = false;

    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean isGlobalScope = false;

    @PrePersist
    public void prePersist() {
        super.prePersist();
        if (status == null) {
            status = 1;
        }
        if (requiresStaffProfile == null) {
            requiresStaffProfile = false;
        }
        if (isGlobalScope == null) {
            isGlobalScope = false;
        }
    }
}
