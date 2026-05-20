package com.piania.core.controller;

import com.piania.core.dto.chat.ChatReadStateResponse;
import com.piania.core.dto.chat.ChatReadStateUpsertRequest;
import com.piania.core.service.ChatReadStateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping({"/chat-read-state", "/piania/chat-read-state"})
public class ChatReadStateController {

    private final ChatReadStateService chatReadStateService;

    @GetMapping("/me/{virtualClassId}")
    public ChatReadStateResponse getMyState(
            @PathVariable Long virtualClassId,
            Authentication authentication
    ) {
        String email = authentication.getName();
        return chatReadStateService.getMyState(virtualClassId, email);
    }

    @PostMapping("/me")
    public ChatReadStateResponse upsertMyState(
            @Valid @RequestBody ChatReadStateUpsertRequest request,
            Authentication authentication
    ) {
        String email = authentication.getName();
        return chatReadStateService.upsertMyState(
                request.virtualClassId(),
                email,
                request.lastSeenMessageId()
        );
    }
}
