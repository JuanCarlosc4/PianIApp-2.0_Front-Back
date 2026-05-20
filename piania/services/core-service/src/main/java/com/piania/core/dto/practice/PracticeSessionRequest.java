package com.piania.core.dto.practice;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PracticeSessionRequest {

    @NotNull
    private Long sheetMusicId;

    @NotBlank
    private String audioUrl;

    @NotNull
    @Min(1)
    private Integer durationSeconds;
}
