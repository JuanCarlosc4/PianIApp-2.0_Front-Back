package com.piania.core.dto.practice;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PracticeFeedbackResponse {

    private Long practiceSessionId;
    private Integer precisionGeneral;
    private Integer noteErrors;
    private Integer rhythmErrors;
    private String detailedReport;
}
