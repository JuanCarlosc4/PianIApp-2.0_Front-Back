package com.piania.core.dto.sheetmusic;

import java.time.LocalDateTime;

public record SheetMusicAnalysisResponse(
        String tonalidad,
        String compas,
        Integer numeroCompases,
        Double dificultadEstimada,
        Integer tempoDetectado,
        LocalDateTime fechaAnalisis
) {}
