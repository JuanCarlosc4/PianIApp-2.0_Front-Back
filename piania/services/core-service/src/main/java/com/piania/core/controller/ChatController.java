package com.piania.core.controller;

import com.piania.core.dto.chat.ChatMessageRequest;
import com.piania.core.dto.chat.ChatMessageResponse;
import com.piania.core.entity.ChatMessage;
import com.piania.core.entity.ClassEnrollment;
import com.piania.core.entity.VirtualClass;
import com.piania.core.enums.EnrollmentStatus;
import com.piania.core.exception.ForbiddenException;
import com.piania.core.exception.NotFoundException;
import com.piania.core.repository.ChatMessageRepository;
import com.piania.core.repository.ClassEnrollmentRepository;
import com.piania.core.repository.VirtualClassRepository;
import com.piania.core.service.ChatCryptoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/piania/classes/{classId}/chat")
@RequiredArgsConstructor
public class ChatController {

    private final VirtualClassRepository virtualClassRepository;
    private final ClassEnrollmentRepository classEnrollmentRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatCryptoService chatCryptoService;

    @GetMapping
    @PreAuthorize("hasAnyRole('TEACHER','USER')")
    public Page<ChatMessageResponse> listMessages(
            @PathVariable Long classId,
            Pageable pageable,
            Authentication authentication
    ) {
        VirtualClass virtualClass = loadAndAuthorize(classId, authentication);

        Page<ChatMessage> page = chatMessageRepository.findByVirtualClassOrderByCreatedAtDesc(virtualClass, pageable);

        List<ChatMessageResponse> content = page.getContent().stream()
                .map((ChatMessage m) -> {
                    // TODO: cuando se integre auth-service, aquí se debe resolver senderName/avatar desde el usuario
                    //       (para ahora devolvemos datos mínimos y dejamos nulls).
                    boolean senderIsTeacher = m.getSenderEmail().equals(virtualClass.getTeacherEmail());
                    return new ChatMessageResponse(
                            m.getId(),
                            classId,
                            m.getSenderEmail(),
                            m.getSenderEmail(), // fallback temporal: se reemplazará por fullName
                            null, // avatar (AVATAR_1..4)
                            chatCryptoService.decrypt(m.getCipherText(), m.getIvBase64()),
                            senderIsTeacher,
                            m.isPinned(),
                            m.getCreatedAt()
                    );
                })
                .toList();

        return new PageImpl<>(content, pageable, page.getTotalElements());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('TEACHER','USER')")
    public ChatMessageResponse sendMessage(
            @PathVariable Long classId,
            @Valid @RequestBody ChatMessageRequest body,
            Authentication authentication
    ) {
        VirtualClass virtualClass = loadAndAuthorize(classId, authentication);

        ChatCryptoService.EncryptedPayload encrypted = chatCryptoService.encrypt(body.message());

        ChatMessage saved = chatMessageRepository.save(
                ChatMessage.builder()
                        .virtualClass(virtualClass)
                        .senderEmail(authentication.getName())
                        .cipherText(encrypted.cipherText())
                        .ivBase64(encrypted.ivBase64())
                        .pinned(false)
                        .build()
        );

        boolean senderIsTeacher = saved.getSenderEmail().equals(virtualClass.getTeacherEmail());
        return new ChatMessageResponse(
                saved.getId(),
                classId,
                saved.getSenderEmail(),
                saved.getSenderEmail(), // fallback temporal
                null, // avatar
                body.message(),
                senderIsTeacher,
                saved.isPinned(),
                saved.getCreatedAt()
        );
    }

    @PatchMapping("/{messageId}/pin")
    @PreAuthorize("hasRole('TEACHER')")
    @Transactional
    public void pinMessage(
            @PathVariable Long classId,
            @PathVariable Long messageId,
            Authentication authentication
    ) {
        VirtualClass virtualClass = loadAndAuthorize(classId, authentication);

        // sólo profesor de la clase
        if (!virtualClass.getTeacherEmail().equals(authentication.getName())) {
            throw new ForbiddenException("Access denied");
        }

        ChatMessage msg = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new NotFoundException("Message not found"));

        if (!msg.getVirtualClass().getId().equals(virtualClass.getId())) {
            throw new ForbiddenException("Access denied");
        }

        // Permitir múltiples mensajes fijados: no desfijar el resto al fijar uno nuevo.
        msg.setPinned(true);
        chatMessageRepository.save(msg);
    }

    @PatchMapping("/{messageId}/unpin")
    @PreAuthorize("hasRole('TEACHER')")
    @Transactional
    public void unpinMessage(
            @PathVariable Long classId,
            @PathVariable Long messageId,
            Authentication authentication
    ) {
        VirtualClass virtualClass = loadAndAuthorize(classId, authentication);

        if (!virtualClass.getTeacherEmail().equals(authentication.getName())) {
            throw new ForbiddenException("Access denied");
        }

        ChatMessage msg = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new NotFoundException("Message not found"));

        if (!msg.getVirtualClass().getId().equals(virtualClass.getId())) {
            throw new ForbiddenException("Access denied");
        }

        msg.setPinned(false);
        chatMessageRepository.save(msg);
    }

    @DeleteMapping("/{messageId}")
    @PreAuthorize("hasAnyRole('TEACHER','USER')")
    @Transactional
    public void deleteMessage(
            @PathVariable Long classId,
            @PathVariable Long messageId,
            Authentication authentication
    ) {
        VirtualClass virtualClass = loadAndAuthorize(classId, authentication);

        ChatMessage msg = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new NotFoundException("Message not found"));

        if (!msg.getVirtualClass().getId().equals(virtualClass.getId())) {
            throw new ForbiddenException("Access denied");
        }

        String email = authentication.getName();
        boolean isTeacher = virtualClass.getTeacherEmail().equals(email);
        boolean isOwner = msg.getSenderEmail().equals(email);

        // profesor puede borrar cualquiera; alumno sólo los suyos
        if (!isTeacher && !isOwner) {
            throw new ForbiddenException("Access denied");
        }

        chatMessageRepository.delete(msg);
    }

    private VirtualClass loadAndAuthorize(Long classId, Authentication authentication) {
        VirtualClass virtualClass = virtualClassRepository.findById(classId)
                .orElseThrow(() -> new NotFoundException("Class not found"));

        String email = authentication.getName();

        // Teacher: solo puede acceder a su clase
        if (virtualClass.getTeacherEmail().equals(email)) {
            return virtualClass;
        }

        // Student: solo si está matriculado y activo
        ClassEnrollment enrollment = classEnrollmentRepository.findByVirtualClassAndStudentEmail(virtualClass, email)
                .orElseThrow(() -> new ForbiddenException("Access denied"));

        if (enrollment.getStatus() != EnrollmentStatus.ACTIVE) {
            throw new ForbiddenException("Access denied");
        }

        return virtualClass;
    }
}
