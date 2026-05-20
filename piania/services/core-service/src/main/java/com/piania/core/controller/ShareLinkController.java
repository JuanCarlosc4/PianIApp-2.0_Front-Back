package com.piania.core.controller;

import com.piania.core.dto.sharelink.ShareLinkCreateRequest;
import com.piania.core.dto.sheetmusic.SheetMusicResponse;
import com.piania.core.dto.sharelink.ShareLinkResponse;
import com.piania.core.entity.ShareLink;
import com.piania.core.entity.SheetMusic;
import com.piania.core.mapper.ShareLinkMapper;
import com.piania.core.service.ShareLinkService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/piania/share-links")
@RequiredArgsConstructor
public class ShareLinkController {

    private final ShareLinkService shareLinkService;
    private final ShareLinkMapper mapper;

    @PostMapping
    @PreAuthorize("hasRole('USER') or hasRole('TEACHER') or hasRole('ADMIN')")
    public ShareLinkResponse createShareLink(@Valid @RequestBody ShareLinkCreateRequest request,
                                             Authentication authentication) {

        ShareLink shareLink = shareLinkService.createShareLink(
                request.getSheetMusicId(),
                request.getAccessType(),
                request.getExpiresAt(),
                authentication.getName()
        );

        return mapper.toResponse(shareLink);
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('USER') or hasRole('TEACHER') or hasRole('ADMIN')")
    public Page<ShareLinkResponse> getMyLinks(Authentication authentication,
                                              Pageable pageable) {

        List<ShareLinkResponse> responses = shareLinkService
                .getMyActiveLinks(authentication.getName())
                .stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());

        return new PageImpl<>(responses, pageable, responses.size());
    }

    @PostMapping("/import/{token}")
    public SheetMusicResponse importSharedSheet(
            @PathVariable String token,
            Authentication authentication
    ) throws IOException {

        String requesterEmail = authentication.getName();

        SheetMusic imported =
                shareLinkService.importSharedSheet(
                        token,
                        requesterEmail
                );

        return SheetMusicResponse.builder()
                .id(imported.getId())
                .title(imported.getTitle())
                .composer(imported.getComposer())
                .ownerEmail(imported.getOwnerEmail())
                .musicXmlUrl(imported.getMusicXmlUrl())
                .status(imported.getStatus())
                .isPublic(imported.isPublic())
                .tonalidad(imported.getTonalidad())
                .compas(imported.getCompas())
                .dificultadEstimada(imported.getDificultadEstimada())
                .numeroCompases(imported.getNumeroCompases())
                .tempoDetectado(imported.getTempoDetectado())
                .analyzedAt(imported.getAnalyzedAt())
                .createdAt(imported.getCreatedAt())
                .build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('USER') or hasRole('TEACHER') or hasRole('ADMIN')")
    public void revoke(@PathVariable Long id,
                       Authentication authentication) {

        shareLinkService.revokeShareLink(id, authentication.getName());
    }

    @PostMapping("/{id}/permissions")
    @PreAuthorize("hasRole('USER') or hasRole('TEACHER') or hasRole('ADMIN')")
    public void addPermission(@PathVariable Long id,
                              @RequestParam String allowedEmail,
                              Authentication authentication) {

        shareLinkService.addPermission(id, allowedEmail, authentication.getName());
    }
}
