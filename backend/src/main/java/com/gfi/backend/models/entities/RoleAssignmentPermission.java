package com.gfi.backend.models.entities;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(
    name = "role_assignment_permissions",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_rap_creator_target",
        columnNames = {"creator_role_id", "target_role_id"}
    )
)
public class RoleAssignmentPermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "creator_role_id", foreignKey = @ForeignKey(name = "fk_rap_creator_role"))
    private Role creatorRole;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "target_role_id", foreignKey = @ForeignKey(name = "fk_rap_target_role"))
    private Role targetRole;

    @Column(name = "can_create", nullable = false)
    private Integer canCreate = 0;

    @Column(name = "can_update", nullable = false)
    private Integer canUpdate = 0;

    @Column(nullable = false)
    private Integer status = 1;

    @Column(name = "deleted_flag", nullable = false)
    private Integer deletedFlag = 0;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "created_by", length = 255)
    private String createdBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_by", length = 255)
    private String updatedBy;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = 1;
        }
        if (deletedFlag == null) {
            deletedFlag = 0;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
