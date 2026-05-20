package com.piania.core.dto.practicefeedback;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record PracticeFeedbackRequest(
        @NotNull Long practiceSessionId,
        @NotNull @DecimalMin("0.0") @DecimalMax("100.0") Double accuracyScore,
        Integer noteErrors,
        Integer rhythmErrors,
        String teacherComment
) {}
