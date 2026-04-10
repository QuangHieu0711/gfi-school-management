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
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "classroom_subjects", uniqueConstraints = {
        @UniqueConstraint(name = "uk_classroom_subjects_classroom_subject", columnNames = { "classroom_id", "subject_id" })
})
@Getter
@Setter
public class ClassroomSubject extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "classroom_id", nullable = false, foreignKey = @ForeignKey(name = "fk_classroom_subjects_classes"))
    private Classroom classroom;

    @ManyToOne
    @JoinColumn(name = "subject_id", nullable = false, foreignKey = @ForeignKey(name = "fk_classroom_subjects_subjects"))
    private Subject subject;

    @Column(nullable = false)
    private Integer status;

    @Column(length = 500)
    private String description;

    @PrePersist
    public void prePersist() {
        super.prePersist();
        if (status == null) {
            status = 1;
        }
    }
}
