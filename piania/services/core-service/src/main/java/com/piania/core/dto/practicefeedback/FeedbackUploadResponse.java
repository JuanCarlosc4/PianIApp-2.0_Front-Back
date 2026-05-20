package com.piania.core.dto.practicefeedback;

import java.time.LocalDateTime;

public record FeedbackUploadResponse(
        Long idFeedback,
        Integer puntuacionGeneral,
        Integer erroresNota,
        Integer erroresRitmo,
        String comentarios,
        LocalDateTime fechaGeneracion
) {}
