package com.piania.core.service;

import com.piania.core.dto.chat.ChatReadStateResponse;
import com.piania.core.entity.ChatReadState;
import com.piania.core.entity.VirtualClass;
import com.piania.core.exception.NotFoundException;
import com.piania.core.repository.ChatReadStateRepository;
import com.piania.core.repository.VirtualClassRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChatReadStateService {

    private final ChatReadStateRepository chatReadStateRepository;
    private final VirtualClassRepository virtualClassRepository;

    @Transactional(readOnly = true)
    public ChatReadStateResponse getMyState(Long virtualClassId, String userEmail) {
        ChatReadState state = chatReadStateRepository
                .findByVirtualClassIdAndUserEmail(virtualClassId, userEmail)
                .orElse(null);

        Long lastSeen = state == null ? null : state.getLastSeenMessageId();
        return new ChatReadStateResponse(virtualClassId, userEmail, lastSeen);
    }

    @Transactional
    public ChatReadStateResponse upsertMyState(Long virtualClassId, String userEmail, Long lastSeenMessageId) {
        VirtualClass vc = virtualClassRepository.findById(virtualClassId)
                .orElseThrow(() -> new NotFoundException("Clase no encontrada"));

        ChatReadState state = chatReadStateRepository
                .findByVirtualClassIdAndUserEmail(virtualClassId, userEmail)
                .orElse(ChatReadState.builder()
                        .virtualClass(vc)
                        .userEmail(userEmail)
                        .lastSeenMessageId(null)
                        .build());

        // Monótono: no permitimos "volver atrás" para evitar inconsistencias.
        if (lastSeenMessageId != null) {
            Long current = state.getLastSeenMessageId();
            if (current == null || lastSeenMessageId > current) {
                state.setLastSeenMessageId(lastSeenMessageId);
            }
        }

        ChatReadState saved = chatReadStateRepository.save(state);
        return new ChatReadStateResponse(virtualClassId, userEmail, saved.getLastSeenMessageId());
    }
}
