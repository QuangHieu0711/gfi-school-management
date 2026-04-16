package com.gfi.backend.models.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

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
    private String passwordHash;

    @ManyToOne
    @JoinColumn(name = "role_id", nullable = false, foreignKey = @ForeignKey(name = "fk_users_roles"))
    private Role role;

    // User has the FK to Staff (one-to-one, optional)
    @OneToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "staff_id", unique = true, nullable = true, foreignKey = @ForeignKey(name = "fk_users_staff"))
    private Staff staff;

    // Track last login time
    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column
    private Integer status;

    // Convenience getters: Profile data now lives in Staff, not User
    public String getFullName() {
        return staff != null ? staff.getFullName() : null;
    }

    public String getEmail() {
        return staff != null ? staff.getEmail() : null;
    }

    public String getPhone() {
        return staff != null ? staff.getPhone() : null;
    }

    public Long getUnitId() {
        return staff != null && staff.getUnit() != null ? staff.getUnit().getId() : null;
    }

    @PrePersist
    public void prePersist() {
        super.prePersist();
        if (status == null) {
            status = 1;
        }
    }
}
