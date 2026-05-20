package com.piania.core.entity;

import com.piania.core.enums.SheetMusicStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "sheet_music")
@Access(AccessType.FIELD)
public class SheetMusic extends BaseEntity {

    @Column(nullable = false)
    private String title;

    private String composer;

    @Column(nullable = false)
    private String ownerEmail;

    @Column(nullable = false)
    private String originalFileUrl;

    private String musicXmlUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SheetMusicStatus status;

    @Column(nullable = false)
    private boolean isPublic;

    private String tonalidad;

    private String compas;

    private Double dificultadEstimada;

    private Integer numeroCompases;

    private Integer tempoDetectado;

    private LocalDateTime analyzedAt;

    @Column(nullable = false)
    private boolean deleted;
}
