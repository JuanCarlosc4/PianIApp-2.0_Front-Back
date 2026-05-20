package com.piania.core.dto.chat;

import java.time.LocalDateTime;

public record ChatMessageResponse(
        Long id,
        Long classId,
        String senderEmail,
        String senderName,
        String senderAvatar,
        String message,
        boolean senderIsTeacher,
        boolean pinned,
        LocalDateTime createdAt
) {}
