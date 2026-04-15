package com.gfi.backend.models.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;

@Entity
@Table(name = "staff_trainings")
@Getter
@Setter
public class StaffTraining {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "staff_id", nullable = false, foreignKey = @ForeignKey(name = "fk_staff_trainings_staff"))
    private Staff staff;

    @Column(nullable = false)
    private Long trainingTypeId;

    @Column(length = 255)
    private String result;

    @Column
    private LocalDate fromDate;

    @Column
    private LocalDate toDate;

    @Column(length = 255)
    private String organizer;

    @Column(length = 100)
    private String certificateNo;

    @Column(columnDefinition = "TEXT")
    private String note;
}
