package com.piania.core.dto.practice;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class PracticeSessionResponse {

    private Long id;
    private Long sheetMusicId;
    private String audioUrl;
    private Integer durationSeconds;
    private Integer score;
    private String studentObservations;
    private String teacherCorrections;
    private Integer listenCount;
    private LocalDateTime createdAt;
}
