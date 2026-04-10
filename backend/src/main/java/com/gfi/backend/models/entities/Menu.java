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
@Table(name = "menus")
public class Menu extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "parent_menu_id", foreignKey = @ForeignKey(name = "fk_menus_parent"))
    private Menu parentMenu;

    @Column(name = "menu_id", nullable = false, unique = true, length = 100)
    private String code;

    @Column(name = "menu_name", nullable = false, length = 255)
    private String name;

    @Column(length = 500)
    private String url;

    @Column(length = 255)
    private String icon;

    @Column(nullable = false)
    private Integer ordinal;

    @PrePersist
    public void prePersist() {
        super.prePersist();
        if (ordinal == null) {
            ordinal = 0;
        }
    }
}
