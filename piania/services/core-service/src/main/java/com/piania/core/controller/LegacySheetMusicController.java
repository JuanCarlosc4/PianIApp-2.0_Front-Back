package com.piania.core.controller;

import com.piania.core.dto.sheetmusic.SheetMusicAnalysisResponse;
import com.piania.core.entity.SheetMusic;
import com.piania.core.service.SheetMusicService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/piania/partituras")
@RequiredArgsConstructor
public class LegacySheetMusicController {

    private final SheetMusicService sheetMusicService;

    @GetMapping("/{id}/ultimo-analisis")
    @PreAuthorize("hasAnyRole('USER','TEACHER','ADMIN')")
    public SheetMusicAnalysisResponse latestAnalysis(@PathVariable Long id, Authentication authentication) {
        SheetMusic sheetMusic = sheetMusicService.getAccessibleById(id, authentication.getName());

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
