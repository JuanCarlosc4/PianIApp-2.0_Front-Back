package com.piania.core.mapper;

import com.piania.core.dto.practice.PracticeFeedbackResponse;
import com.piania.core.entity.PracticeFeedback;

public class PracticeFeedbackMapper {

    private PracticeFeedbackMapper() {
    }

    public static PracticeFeedbackResponse toResponse(PracticeFeedback entity) {
        if (entity == null) {
            return null;
        }

        return PracticeFeedbackResponse.builder()
                .practiceSessionId(entity.getPracticeSessionId())
                .precisionGeneral(entity.getPrecisionGeneral())
                .noteErrors(entity.getNoteErrors())
                .rhythmErrors(entity.getRhythmErrors())
                .detailedReport(entity.getDetailedReport())
                .build();
    }
}
