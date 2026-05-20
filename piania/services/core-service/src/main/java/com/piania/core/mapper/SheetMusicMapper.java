package com.piania.core.mapper;

import com.piania.core.dto.sheetmusic.SheetMusicResponse;
import com.piania.core.entity.SheetMusic;

public class SheetMusicMapper {

    private SheetMusicMapper() {
    }

    public static SheetMusicResponse toResponse(SheetMusic entity) {
        return SheetMusicResponse.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .composer(entity.getComposer())
                .ownerEmail(entity.getOwnerEmail())
                .musicXmlUrl(entity.getMusicXmlUrl())
                .status(entity.getStatus())
                .isPublic(entity.isPublic())
                .tonalidad(entity.getTonalidad())
                .compas(entity.getCompas())
                .dificultadEstimada(entity.getDificultadEstimada())
                .numeroCompases(entity.getNumeroCompases())
                .tempoDetectado(entity.getTempoDetectado())
                .analyzedAt(entity.getAnalyzedAt())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
