package com.piania.core.dto.playbackhistory;

import java.time.LocalDateTime;

public record PlaybackHistoryResponse(
        Long id,
        String userEmail,
        Long sheetMusicId,
        String sheetMusicTitle,
        LocalDateTime playedAt,
        LocalDateTime createdAt
) {}
