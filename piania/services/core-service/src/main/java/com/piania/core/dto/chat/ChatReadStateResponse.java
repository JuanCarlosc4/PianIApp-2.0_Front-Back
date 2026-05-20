package com.piania.core.dto.chat;

public record ChatReadStateResponse(
        Long virtualClassId,
        String userEmail,
        Long lastSeenMessageId
) {
}
