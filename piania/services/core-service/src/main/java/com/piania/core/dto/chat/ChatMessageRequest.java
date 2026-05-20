package com.piania.core.dto.chat;

import jakarta.validation.constraints.NotBlank;

public record ChatMessageRequest(
        @NotBlank String message
) {}
