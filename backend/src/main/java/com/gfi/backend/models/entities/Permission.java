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
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "permissions", uniqueConstraints = {
        @UniqueConstraint(name = "uk_permissions_role_menu", columnNames = { "role_id", "menu_id" })
})
public class Permission extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer isAdd;

    @Column(nullable = false)
    private Integer isDelete;

    @Column(nullable = false)
    private Integer isDownload;

    @Column(nullable = false)
    private Integer isConfig;

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

    @PrePersist
    public void prePersist() {
        super.prePersist();
        isAdd = defaultZero(isAdd);
        isDelete = defaultZero(isDelete);
        isDownload = defaultZero(isDownload);
        isConfig = defaultZero(isConfig);
        isEdit = defaultZero(isEdit);
        isView = defaultZero(isView);
    }

    private Integer defaultZero(Integer value) {
        return value == null ? 0 : value;
    }
}