package com.piania.core.dto.chat;

import jakarta.validation.constraints.NotNull;

public record ChatReadStateUpsertRequest(
        @NotNull Long virtualClassId,
        Long lastSeenMessageId
) {
}
