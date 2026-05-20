package com.piania.core.entity;

import com.piania.core.enums.ClassStatus;
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
@Table(name = "virtual_classes")
public class VirtualClass extends BaseEntity {

    @Column(nullable = false)
    private String teacherEmail;

    @Column(nullable = false)
    private String name;

    /**
     * Avatar del grupo/clase. No se puede subir una imagen propia:
     * se elige de un set fijo (p.ej. GROUP_AVATAR_1..N) que el frontend renderiza.
     */
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private GroupAvatar groupAvatar;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ClassStatus status;
}
