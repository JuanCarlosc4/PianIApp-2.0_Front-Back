package com.piania.core.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "teacher_student_relation",
       uniqueConstraints = @UniqueConstraint(columnNames = {"teacherEmail", "studentEmail"}))
public class TeacherStudentRelation extends BaseEntity {

    @Column(nullable = false)
    private String teacherEmail;

    @Column(nullable = false)
    private String studentEmail;

    @Column(nullable = false)
    private boolean active;
}
