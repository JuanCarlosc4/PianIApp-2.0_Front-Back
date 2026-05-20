package com.piania.core.dto.sheetmusic;

import com.piania.core.enums.SheetMusicStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class SheetMusicResponse {

    private Long id;
    private String title;
    private String composer;
    private String ownerEmail;
    private String musicXmlUrl;
    private SheetMusicStatus status;
    private boolean isPublic;
    private String tonalidad;
    private String compas;
    private Double dificultadEstimada;
    private Integer numeroCompases;
    private Integer tempoDetectado;
    private LocalDateTime analyzedAt;
    private LocalDateTime createdAt;
}
