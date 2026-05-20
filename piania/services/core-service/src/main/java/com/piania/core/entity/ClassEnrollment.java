package com.piania.core.entity;

import com.piania.core.enums.EnrollmentStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "class_enrollments",
       uniqueConstraints = @UniqueConstraint(columnNames = {"virtual_class_id", "studentEmail"}))
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ClassEnrollment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "virtual_class_id", nullable = false)
    private VirtualClass virtualClass;

    @Column(nullable = false)
    private String studentEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private EnrollmentStatus status = EnrollmentStatus.ACTIVE;
}
