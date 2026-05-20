package com.piania.core.service;

import com.piania.core.entity.ShareLink;
import com.piania.core.enums.SheetMusicStatus;
import com.piania.core.entity.ShareLinkPermission;
import com.piania.core.entity.SheetMusic;
import com.piania.core.enums.ShareAccessType;
import com.piania.core.exception.ForbiddenException;
import com.piania.core.exception.NotFoundException;
import com.piania.core.repository.ShareLinkPermissionRepository;
import com.piania.core.repository.ShareLinkRepository;
import com.piania.core.repository.SheetMusicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import java.io.IOException;

@Service
@RequiredArgsConstructor
public class ShareLinkService {

    private final ShareLinkRepository shareLinkRepository;
    private final ShareLinkPermissionRepository permissionRepository;
    private final SheetMusicRepository sheetMusicRepository;

    @Transactional
    public ShareLink createShareLink(Long sheetMusicId,
                                     ShareAccessType accessType,
                                     LocalDateTime expiresAt,
                                     String ownerEmail) {

        SheetMusic sheetMusic = sheetMusicRepository.findById(sheetMusicId)
                .orElseThrow(() -> new NotFoundException("Sheet music not found"));

        if (!sheetMusic.getOwnerEmail().equals(ownerEmail)) {
            throw new ForbiddenException("You are not the owner of this sheet music");
        }

        ShareLink shareLink = ShareLink.builder()
                .sheetMusic(sheetMusic)
                .ownerEmail(ownerEmail)
                .accessType(accessType)
                .expiresAt(expiresAt)
                .active(true)
                .build();

        return shareLinkRepository.save(shareLink);
    }

    @Transactional
    public SheetMusic importSharedSheet(
            String token,
            String requesterEmail
    ) throws IOException {

        ShareLink shareLink = shareLinkRepository.findByToken(token)
                .orElseThrow(() ->
                        new NotFoundException("Share link not found"));

        if (!shareLink.isActive()) {
            throw new ForbiddenException("Share link inactive");
        }

        SheetMusic original = shareLink.getSheetMusic();

        if (original == null) {
            throw new NotFoundException("Original sheet music not found");
        }

        // =========================
        // SOURCE FILE
        // =========================

        String relativePath = original.getMusicXmlUrl();

        if (relativePath == null || relativePath.isBlank()) {
            throw new NotFoundException("MusicXML URL is null");
        }

        relativePath = relativePath.trim();

        if (relativePath.startsWith("/")) {
            relativePath = relativePath.substring(1);
        }

        Path sourcePath = Paths.get("/app", relativePath);

        System.out.println("========== IMPORT DEBUG ==========");
        System.out.println("musicXmlUrl: " + original.getMusicXmlUrl());
        System.out.println("relativePath: " + relativePath);
        System.out.println("sourcePath: " + sourcePath.toAbsolutePath());
        System.out.println("exists: " + Files.exists(sourcePath));
        System.out.println("==================================");

        if (!Files.exists(sourcePath)) {
            throw new NotFoundException(
                    "Musicxml file not found: " + sourcePath
            );
        }

        // =========================
        // CREATE NEW SHEET
        // =========================

        SheetMusic imported = SheetMusic.builder()
                .title(original.getTitle())
                .composer(original.getComposer())
                .ownerEmail(requesterEmail)
                .status(SheetMusicStatus.READY)
                .isPublic(false)
                .deleted(false)
                .originalFileUrl(original.getOriginalFileUrl())
                .tonalidad(original.getTonalidad())
                .compas(original.getCompas())
                .dificultadEstimada(original.getDificultadEstimada())
                .numeroCompases(original.getNumeroCompases())
                .tempoDetectado(original.getTempoDetectado())
                .analyzedAt(original.getAnalyzedAt())
                .build();

        imported = sheetMusicRepository.save(imported);

        // =========================
        // TARGET FILE
        // =========================

        Path targetDir = Paths.get(
                "/app/uploads/sheet-music/" + imported.getId()
        );

        Files.createDirectories(targetDir);

        Path targetPath = targetDir.resolve("score.musicxml");

        Files.copy(
                sourcePath,
                targetPath,
                StandardCopyOption.REPLACE_EXISTING
        );

        // =========================
        // SAVE NEW URL
        // =========================

        imported.setMusicXmlUrl(
                "/uploads/sheet-music/" +
                imported.getId() +
                "/score.musicxml"
        );

        return sheetMusicRepository.save(imported);
    }

    @Transactional(readOnly = true)
    public SheetMusic accessByToken(String token, String requesterEmail) {

        ShareLink shareLink = shareLinkRepository.findByToken(token)
                .orElseThrow(() -> new NotFoundException("Share link not found"));

        if (!shareLink.isActive()) {
            throw new ForbiddenException("Share link is inactive");
        }

        if (shareLink.getExpiresAt() != null &&
                shareLink.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ForbiddenException("Share link has expired");
        }

        switch (shareLink.getAccessType()) {
            case PUBLIC:
                return shareLink.getSheetMusic();

            case PRIVATE:
                if (!shareLink.getOwnerEmail().equals(requesterEmail)) {
                    throw new ForbiddenException("Access denied");
                }
                return shareLink.getSheetMusic();

            case RESTRICTED:
                if (shareLink.getOwnerEmail().equals(requesterEmail)) {
                    return shareLink.getSheetMusic();
                }

                permissionRepository.findByShareLinkIdAndAllowedEmail(
                        shareLink.getId(),
                        requesterEmail
                ).orElseThrow(() -> new ForbiddenException("Access denied"));

                return shareLink.getSheetMusic();

            default:
                throw new ForbiddenException("Invalid access type");
        }
    }

    @Transactional
    public void revokeShareLink(Long shareLinkId, String requesterEmail) {

        ShareLink shareLink = shareLinkRepository.findById(shareLinkId)
                .orElseThrow(() -> new NotFoundException("Share link not found"));

        if (!shareLink.getOwnerEmail().equals(requesterEmail)) {
            throw new ForbiddenException("You are not the owner of this share link");
        }

        shareLink.setActive(false);
        shareLinkRepository.save(shareLink);
    }

    @Transactional
    public void addPermission(Long shareLinkId, String allowedEmail, String requesterEmail) {

        ShareLink shareLink = shareLinkRepository.findById(shareLinkId)
                .orElseThrow(() -> new NotFoundException("Share link not found"));

        if (!shareLink.getOwnerEmail().equals(requesterEmail)) {
            throw new ForbiddenException("You are not the owner of this share link");
        }

        if (shareLink.getAccessType() != ShareAccessType.RESTRICTED) {
            throw new ForbiddenException("Permissions only allowed for RESTRICTED links");
        }

        ShareLinkPermission permission = ShareLinkPermission.builder()
                .shareLink(shareLink)
                .allowedEmail(allowedEmail)
                .build();

        permissionRepository.save(permission);
    }

    @Transactional(readOnly = true)
    public List<ShareLink> getMyActiveLinks(String ownerEmail) {
        return shareLinkRepository.findByOwnerEmailAndActiveTrue(ownerEmail);
    }
}
