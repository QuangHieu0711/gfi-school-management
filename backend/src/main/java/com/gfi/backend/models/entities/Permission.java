package com.gfi.backend.models.entities;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "permissions", uniqueConstraints = {
        @UniqueConstraint(name = "uk_permissions_role_menu", columnNames = { "role_id", "menu_id" })
})
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer isAdd;

    @Column(nullable = false)
    private Integer isApprove;

    @Column(nullable = false)
    private Integer isDelete;

    @Column(nullable = false)
    private Integer isDownload;

    @Column(nullable = false)
    private Integer isEdit;

    @Column(nullable = false)
    private Integer isView;

    @ManyToOne
    @JoinColumn(name = "menu_id", nullable = false, foreignKey = @ForeignKey(name = "fk_permissions_menus"))
    private Menu menu;

    @ManyToOne
    @JoinColumn(name = "role_id", nullable = false, foreignKey = @ForeignKey(name = "fk_permissions_roles"))
    private Role role;

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
        if (isAdd == null) {
            isAdd = 0;
        }
        if (isApprove == null) {
            isApprove = 0;
        }
        if (isDelete == null) {
            isDelete = 0;
        }
        if (isDownload == null) {
            isDownload = 0;
        }
        if (isEdit == null) {
            isEdit = 0;
        }
        if (isView == null) {
            isView = 0;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
