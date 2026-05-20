package com.piania.core.service;

import com.piania.core.entity.SheetMusic;
import com.piania.core.enums.SheetMusicStatus;
import com.piania.core.exception.BadRequestException;
import com.piania.core.repository.SheetMusicRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.net.URI;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OmrIntegrationService {

    private final WebClient omrWebClient;
    private final WebClient genericWebClient;
    private final SheetMusicRepository sheetMusicRepository;

    @Value("${piania.uploads.dir:uploads}")
    private String uploadsDir;

    private static final String PUBLIC_UPLOADS_PREFIX = "/uploads/";
    private static final String SHEET_MUSIC_DIR = "sheet-music";

    @Async
    public void processSheetMusicAsync(Long sheetMusicId) {
        try {
            processSheetMusic(sheetMusicId);
        } catch (RuntimeException ex) {
            log.warn("Asynchronous OMR processing failed for sheet music {}", sheetMusicId, ex);
        }
    }

    public void processSheetMusic(Long sheetMusicId) {

        SheetMusic sheetMusic = sheetMusicRepository.findById(sheetMusicId)
                .orElseThrow(() -> new BadRequestException("Sheet music not found"));

        if (sheetMusic.getStatus() != SheetMusicStatus.UPLOADED) {
            throw new BadRequestException("Sheet music already processed or in progress");
        }

        sheetMusic.setStatus(SheetMusicStatus.PROCESSING);
        sheetMusicRepository.save(sheetMusic);

        byte[] fileBytes = null;

        try {
            String originalFileUrl = sheetMusic.getOriginalFileUrl();
            if (originalFileUrl == null || originalFileUrl.isBlank()) {
                throw new BadRequestException("Sheet music original file URL is missing");
            }

            fileBytes = readOriginalFile(originalFileUrl);

            if (fileBytes == null) {
                throw new BadRequestException("Unable to download original file");
            }

            String lowerUrl = originalFileUrl.toLowerCase();

            boolean isXml = lowerUrl.endsWith(".xml") || lowerUrl.endsWith(".musicxml") || lowerUrl.contains(".musicxml?") || lowerUrl.contains(".xml?");
            boolean isPdfOrImage = lowerUrl.endsWith(".pdf")
                    || lowerUrl.endsWith(".png")
                    || lowerUrl.endsWith(".jpg")
                    || lowerUrl.endsWith(".jpeg")
                    || lowerUrl.contains(".pdf?")
                    || lowerUrl.contains(".png?")
                    || lowerUrl.contains(".jpg?")
                    || lowerUrl.contains(".jpeg?");

            if (!isXml && !isPdfOrImage) {
                throw new BadRequestException("Unsupported sheet music file type");
            }

            String endpoint = isPdfOrImage ? "/analysis/omr/" + sheetMusicId : "/analysis/score";
            String filename = isPdfOrImage ? getMultipartFilename(originalFileUrl, "sheet.pdf") : "sheet.musicxml";

            Map response = omrWebClient.post()
                    .uri(endpoint)
                    .body(org.springframework.web.reactive.function.BodyInserters
                            .fromMultipartData("file",
                                    new org.springframework.core.io.ByteArrayResource(fileBytes) {
                                        @Override
                                        public String getFilename() {
                                            return filename;
                                        }
                                    }))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block(Duration.ofSeconds(isPdfOrImage ? 300 : 60));

            if (response == null) {
                throw new BadRequestException("OMR processing returned invalid response");
            }

            sheetMusic.setTonalidad((String) response.get("tonalidad"));
            sheetMusic.setCompas((String) response.get("compas"));
            sheetMusic.setNumeroCompases(toInteger(response.get("numeroCompases")));
            sheetMusic.setDificultadEstimada(toDouble(response.get("dificultadEstimada")));
            sheetMusic.setTempoDetectado(toInteger(response.get("tempoDetectado")));
            sheetMusic.setAnalyzedAt(LocalDateTime.now());

            if (isPdfOrImage) {
                Object musicXml = response.get("musicxml");
                if (musicXml == null || musicXml.toString().isBlank()) {
                    throw new BadRequestException("OMR processing returned no MusicXML content");
                }
                
                // Save MusicXML locally first
                String localUrl = saveMusicXml(sheetMusicId, musicXml.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
                
                // But also check if OMR returned a downloadable path
                Object musicxmlPath = response.get("musicxmlPath");
                if (musicxmlPath != null && !musicxmlPath.toString().isBlank()) {
                    // OMR service returned a path like "/analysis/download/{sheet_music_id}"
                    // Prepend the OMR service base URL so client can download from there
                    String omrServicePath = musicxmlPath.toString();
                    // For now, use the local saved URL which is more reliable
                    sheetMusic.setMusicXmlUrl(localUrl);
                } else {
                    sheetMusic.setMusicXmlUrl(localUrl);
                }
            } else {
                sheetMusic.setMusicXmlUrl(saveMusicXml(sheetMusicId, fileBytes));
            }

            sheetMusic.setStatus(SheetMusicStatus.READY);

        } catch (WebClientResponseException ex) {
            sheetMusic.setStatus(SheetMusicStatus.UPLOADED);
            sheetMusicRepository.save(sheetMusic);

            String responseBody = ex.getResponseBodyAsString();
            if (responseBody != null && responseBody.length() > 500) {
                responseBody = responseBody.substring(0, 500) + "...";
            }

            throw new BadRequestException(
                    "OMR service error: " + ex.getStatusCode()
                            + (responseBody != null && !responseBody.isBlank() ? " - " + responseBody : "")
            );
        } catch (Exception ex) {
            sheetMusic.setStatus(SheetMusicStatus.UPLOADED);
            sheetMusicRepository.save(sheetMusic);
            throw new BadRequestException("OMR processing failed: " + ex.getMessage());
        }

        sheetMusicRepository.save(sheetMusic);
    }

    private byte[] readOriginalFile(String originalFileUrl) {
        Optional<String> uploadRelativePath = extractUploadRelativePath(originalFileUrl);

        if (uploadRelativePath.isPresent()) {
            try {
                Path filePath = resolveUploadPath(uploadRelativePath.get());
                return Files.readAllBytes(filePath);
            } catch (IOException ex) {
                throw new BadRequestException("Unable to read uploaded file: " + ex.getMessage());
            }
        }

        return genericWebClient.get()
                .uri(URI.create(originalFileUrl))
                .retrieve()
                .bodyToMono(byte[].class)
                .block(Duration.ofSeconds(30));
    }

    private String saveMusicXml(Long sheetMusicId, byte[] musicXmlBytes) {
        try {
            String relativePath = SHEET_MUSIC_DIR + "/" + sheetMusicId + "/score.musicxml";
            Path target = resolveUploadPath(relativePath);
            Files.createDirectories(target.getParent());
            Files.write(target, musicXmlBytes);
            return PUBLIC_UPLOADS_PREFIX + relativePath;
        } catch (IOException ex) {
            throw new BadRequestException("Unable to save MusicXML locally: " + ex.getMessage());
        }
    }

    private Path resolveUploadPath(String relativePath) {
        Path baseDir = Paths.get(uploadsDir).toAbsolutePath().normalize();
        Path target = baseDir.resolve(relativePath).normalize();

        if (!target.startsWith(baseDir)) {
            throw new BadRequestException("Invalid upload path");
        }

        return target;
    }

    private Optional<String> extractUploadRelativePath(String url) {
        String candidate = url == null ? "" : url.trim();
        if (candidate.isBlank()) {
            return Optional.empty();
        }

        if (candidate.startsWith("http://") || candidate.startsWith("https://")) {
            candidate = URI.create(candidate).getPath();
        }

        if (candidate.startsWith(PUBLIC_UPLOADS_PREFIX)) {
            return Optional.of(candidate.substring(PUBLIC_UPLOADS_PREFIX.length()));
        }

        if (candidate.startsWith("uploads/")) {
            return Optional.of(candidate.substring("uploads/".length()));
        }

        return Optional.empty();
    }

    private String getMultipartFilename(String originalFileUrl, String fallback) {
        String lowerUrl = originalFileUrl == null ? "" : originalFileUrl.toLowerCase();
        if (lowerUrl.endsWith(".png") || lowerUrl.contains(".png?")) {
            return "sheet.png";
        }
        if (lowerUrl.endsWith(".jpg") || lowerUrl.endsWith(".jpeg") || lowerUrl.contains(".jpg?") || lowerUrl.contains(".jpeg?")) {
            return "sheet.jpg";
        }
        return fallback;
    }

    private Integer toInteger(Object value) {
        return Optional.ofNullable(value)
                .map(Object::toString)
                .filter(s -> !s.isBlank())
                .map(Double::valueOf)
                .map(d -> (int) Math.round(d))
                .orElse(null);
    }

    private Double toDouble(Object value) {
        return Optional.ofNullable(value)
                .map(Object::toString)
                .filter(s -> !s.isBlank())
                .map(Double::valueOf)
                .orElse(null);
    }
}
