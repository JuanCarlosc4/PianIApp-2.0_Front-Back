package com.piania.core.dto.userprogress;

import java.time.LocalDateTime;

public record UserProgressResponse(
        Long id,
        String userEmail,
        Long sheetMusicId,
        String sheetMusicTitle,
        Integer totalPracticeMinutes,
        Integer totalSessions,
        Double averageScore,
        LocalDateTime lastPracticeAt,
        LocalDateTime createdAt
) {}
