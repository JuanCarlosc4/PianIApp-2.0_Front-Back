package com.piania.core.controller;

import com.piania.core.dto.practice.PracticeSessionRequest;
import com.piania.core.dto.practice.PracticeSessionResponse;
import com.piania.core.dto.practice.PracticeSessionUpdateRequest;
import com.piania.core.dto.practice.PracticeFeedbackResponse;
import com.piania.core.entity.PracticeFeedback;
import com.piania.core.entity.PracticeSession;
import com.piania.core.mapper.PracticeFeedbackMapper;
import com.piania.core.mapper.PracticeSessionMapper;
import com.piania.core.service.PracticeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/piania/practices")
@RequiredArgsConstructor
public class PracticeController {

    private final PracticeService practiceService;

    @PostMapping
    @PreAuthorize("hasAnyRole('USER','TEACHER','ADMIN')")
    public PracticeSessionResponse createPractice(
            @Valid @RequestBody PracticeSessionRequest request,
            Authentication authentication) {

        PracticeSession session = practiceService.createPractice(
                authentication.getName(),
                request
        );

        return PracticeSessionMapper.toResponse(session);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('USER','TEACHER','ADMIN')")
    public Page<PracticeSessionResponse> listUserPractices(
            Pageable pageable,
            Authentication authentication) {

        Page<PracticeSession> page = practiceService
                .getUserPractices(authentication.getName(), pageable);

        return new PageImpl<>(
                page.getContent().stream()
                        .map(PracticeSessionMapper::toResponse)
                        .toList(),
                pageable,
                page.getTotalElements()
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER','TEACHER','ADMIN')")
    public PracticeSessionResponse getPractice(
            @PathVariable Long id,
            Authentication authentication) {

        PracticeSession session = practiceService.getPracticeById(
                id,
                authentication.getName()
        );

        return PracticeSessionMapper.toResponse(session);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER','TEACHER','ADMIN')")
    public PracticeSessionResponse updatePracticeNotes(
            @PathVariable Long id,
            @Valid @RequestBody PracticeSessionUpdateRequest request,
            Authentication authentication) {

        boolean isTeacher = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_TEACHER") || a.getAuthority().equals("ROLE_ADMIN"));

        PracticeSession updated = practiceService.updatePracticeNotes(
                id,
                authentication.getName(),
                isTeacher,
                request.getStudentObservations(),
                request.getTeacherCorrections()
        );

        return PracticeSessionMapper.toResponse(updated);
    }

    @GetMapping("/by-sheet-music/{sheetMusicId}")
    @PreAuthorize("hasAnyRole('USER','TEACHER','ADMIN')")
    public Page<PracticeSessionResponse> listPracticesBySheetMusic(
            @PathVariable Long sheetMusicId,
            Pageable pageable,
            Authentication authentication) {

        Page<PracticeSession> page = practiceService.getPracticesBySheetMusic(
                sheetMusicId,
                authentication.getName(),
                pageable
        );

        return new PageImpl<>(
                page.getContent().stream()
                        .map(PracticeSessionMapper::toResponse)
                        .toList(),
                pageable,
                page.getTotalElements()
        );
    }

    @GetMapping("/{id}/feedback")
    @PreAuthorize("hasAnyRole('USER','TEACHER','ADMIN')")
    public PracticeFeedbackResponse getPracticeFeedback(
            @PathVariable Long id,
            Authentication authentication) {

        boolean isTeacher = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_TEACHER") || a.getAuthority().equals("ROLE_ADMIN"));

        PracticeFeedback feedback = practiceService.getPracticeFeedback(
                id,
                authentication.getName(),
                isTeacher
        );

        return PracticeFeedbackMapper.toResponse(feedback);
    }
}
