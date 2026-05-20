package com.piania.core.dto.practicefeedback;

import java.time.LocalDateTime;

public record PracticeFeedbackResponse(
        Long id,
        Long practiceSessionId,
        Double accuracyScore,
        Integer noteErrors,
        Integer rhythmErrors,
        String teacherComment,
        String generatedBy,
        LocalDateTime createdAt
) {}
