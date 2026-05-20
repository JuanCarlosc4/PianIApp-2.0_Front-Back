package com.piania.core.controller;

import com.piania.core.dto.sheetmusic.SheetMusicRequest;
import com.piania.core.dto.sheetmusic.SheetMusicAnalysisResponse;
import com.piania.core.dto.sheetmusic.SheetMusicResponse;
import com.piania.core.entity.SheetMusic;
import com.piania.core.service.OmrIntegrationService;
import com.piania.core.service.SheetMusicService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/piania/sheet-music")
@RequiredArgsConstructor
public class SheetMusicController {

    private final SheetMusicService sheetMusicService;
    private final OmrIntegrationService omrIntegrationService;
    

    @PostMapping
    @PreAuthorize("hasAnyRole('USER','TEACHER','ADMIN')")
    public SheetMusicResponse upload(
            @Valid @RequestBody SheetMusicRequest request,
            Authentication authentication) {

        SheetMusic sheetMusic = sheetMusicService.create(authentication.getName(), request);
        omrIntegrationService.processSheetMusicAsync(sheetMusic.getId());

        return com.piania.core.mapper.SheetMusicMapper.toResponse(
                sheetMusic
        );
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('USER','TEACHER','ADMIN')")
    public Page<SheetMusicResponse> list(
            Pageable pageable,
            Authentication authentication) {

        org.springframework.data.domain.Page<com.piania.core.entity.SheetMusic> page =
                sheetMusicService.getUserSheetMusic(authentication.getName(), pageable);

        return new org.springframework.data.domain.PageImpl<>(
                page.getContent().stream()
                        .map(com.piania.core.mapper.SheetMusicMapper::toResponse)
                        .toList(),
                pageable,
                page.getTotalElements()
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER','TEACHER','ADMIN')")
    public SheetMusicResponse get(@PathVariable Long id, Authentication authentication) {
        return com.piania.core.mapper.SheetMusicMapper.toResponse(
                sheetMusicService.getAccessibleById(id, authentication.getName())
        );
    }

    @GetMapping("/{id}/analysis")
    @PreAuthorize("hasAnyRole('USER','TEACHER','ADMIN')")
    public SheetMusicAnalysisResponse analysis(@PathVariable Long id, Authentication authentication) {
        return toAnalysisResponse(sheetMusicService.getAccessibleById(id, authentication.getName()));
    }

    @PostMapping("/{id}/process")
    @PreAuthorize("hasAnyRole('USER','TEACHER','ADMIN')")
    public void process(@PathVariable Long id, Authentication authentication) {
        sheetMusicService.getOwnedById(id, authentication.getName());
        omrIntegrationService.processSheetMusic(id);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable Long id, Authentication authentication) {
        sheetMusicService.delete(id, authentication.getName());
    }

    private SheetMusicAnalysisResponse toAnalysisResponse(SheetMusic sheetMusic) {

        return new SheetMusicAnalysisResponse(
                sheetMusic.getTonalidad(),
                sheetMusic.getCompas(),
                sheetMusic.getNumeroCompases(),
                sheetMusic.getDificultadEstimada(),
                sheetMusic.getTempoDetectado(),
                sheetMusic.getAnalyzedAt()
        );
    }
}
