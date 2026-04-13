package com.gfi.backend.models.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@NoArgsConstructor
@Getter
@Setter
@AllArgsConstructor
@Table(
    name = "role_assignment_permissions",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_rap_creator_target",
        columnNames = {"creator_role_id", "target_role_id"}
    )
)
public class RoleAssignmentPermission extends BaseEntity {

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
}
