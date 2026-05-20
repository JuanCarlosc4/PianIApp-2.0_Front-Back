package com.piania.core.mapper;

import com.piania.core.dto.practice.PracticeSessionResponse;
import com.piania.core.entity.PracticeSession;

public class PracticeSessionMapper {

    private PracticeSessionMapper() {
    }

    public static PracticeSessionResponse toResponse(PracticeSession entity) {
        return PracticeSessionResponse.builder()
                .id(entity.getId())
                .sheetMusicId(entity.getSheetMusic().getId())
                .audioUrl(entity.getAudioUrl())
                .durationSeconds(entity.getDurationSeconds())
                .score(entity.getScore())
                .studentObservations(entity.getStudentObservations())
                .teacherCorrections(entity.getTeacherCorrections())
                .listenCount(entity.getListenCount())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
