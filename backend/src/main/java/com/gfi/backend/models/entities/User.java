package com.gfi.backend.models.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
@Table(name = "users")
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String fullName;

    @Column(length = 100)
    private String email;

    @Column(length = 50)
    private String phone;

    @ManyToOne
    @JoinColumn(name = "role_id", nullable = false, foreignKey = @ForeignKey(name = "fk_users_roles"))
    private Role role;

    @ManyToOne
    @JoinColumn(name = "unit_id", nullable = false, foreignKey = @ForeignKey(name = "fk_users_units"))
    private Unit unit;

    @Column
    private Integer status;

    @PrePersist
    public void prePersist() {
        super.prePersist();
        if (status == null) {
            status = 1;
        }
    }
}
