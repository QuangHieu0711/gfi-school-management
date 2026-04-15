package com.gfi.backend.models.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "staff_addresses", uniqueConstraints = {
        @UniqueConstraint(name = "uk_staff_address_type", columnNames = { "staff_id", "address_type" })
})
@Getter
@Setter
public class StaffAddress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "staff_id", nullable = false, foreignKey = @ForeignKey(name = "fk_staff_addresses_staff"))
    private Staff staff;

    @Column(nullable = false, length = 30)
    private String addressType;

    @Column
    private Long provinceId;

    @Column
    private Long districtId;

    @Column
    private Long wardId;

    @Column(length = 255)
    private String hamletName;

    @Column(length = 500)
    private String detailAddress;

    @Column(length = 500)
    private String fullAddress;
}
