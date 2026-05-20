package com.piania.core.controller;

import com.piania.core.dto.practicefeedback.FeedbackUploadResponse;
import com.piania.core.entity.PracticeFeedback;
import com.piania.core.entity.PracticeSession;
import com.piania.core.exception.BadRequestException;
import com.piania.core.repository.PracticeFeedbackRepository;
import com.piania.core.repository.PracticeSessionRepository;
import com.piania.core.service.PracticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/piania/feedback")
@RequiredArgsConstructor
public class FeedbackController {

    private final PracticeService practiceService;
    private final PracticeSessionRepository practiceSessionRepository;
    private final PracticeFeedbackRepository practiceFeedbackRepository;
    private final WebClient omrWebClient;

    @Value("${piania.uploads.dir:uploads}")
    private String uploadsDir;

    @PostMapping(value = "/upload/{idPartitura}", consumes = {"multipart/form-data"})
    @PreAuthorize("hasAnyRole('USER','TEACHER','ADMIN')")
    public FeedbackUploadResponse uploadFeedback(@PathVariable Long idPartitura,
                                                 @RequestPart("file") MultipartFile file,
                                                 @RequestParam(required = false) Integer durationSeconds,
                                                 Authentication authentication) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Audio file is required");
        }

        String originalName = StringUtils.cleanPath(file.getOriginalFilename() == null ? "audio" : file.getOriginalFilename());
        String extension = extensionOf(originalName);
        if (!List.of(".m4a", ".wav", ".3gp", ".mp3").contains(extension.toLowerCase())) {
            throw new BadRequestException("Invalid audio file type. Must be M4A, WAV, 3GP or MP3.");
        }

        String relativePath = "feedback/" + idPartitura + "/" + UUID.randomUUID() + extension;
        Path target = resolveUploadPath(relativePath);
        Files.createDirectories(target.getParent());
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

        String audioUrl = "/uploads/" + relativePath;
        PracticeSession practiceSession = practiceService.createPracticeFromUpload(
                authentication.getName(),
                idPartitura,
                audioUrl,
                durationSeconds
        );

        List<?> detectedNotes = transcribeAudio(file.getBytes(), originalName);
        int noteCount = detectedNotes == null ? 0 : detectedNotes.size();
        int score = noteCount > 0 ? 85 : 0;
        int noteErrors = noteCount > 0 ? 0 : 1;
        int rhythmErrors = noteCount > 0 ? 0 : 1;
        String comments = noteCount > 0
                ? "Audio transcrito correctamente por OMR. Comparacion fina pendiente de motor de evaluacion."
                : "No se detectaron notas en el audio.";

        practiceSession.setScore(score);
        practiceSessionRepository.save(practiceSession);

        PracticeFeedback feedback = PracticeFeedback.builder()
                .practiceSessionId(practiceSession.getId())
                .practiceSession(practiceSession)
                .precisionGeneral(score)
                .noteErrors(noteErrors)
                .rhythmErrors(rhythmErrors)
                .detailedReport(comments)
                .build();
        practiceFeedbackRepository.save(feedback);

        return new FeedbackUploadResponse(
                feedback.getPracticeSessionId(),
                feedback.getPrecisionGeneral(),
                feedback.getNoteErrors(),
                feedback.getRhythmErrors(),
                feedback.getDetailedReport(),
                practiceSession.getCreatedAt()
        );
    }

    private List<?> transcribeAudio(byte[] audioBytes, String filename) {
        try {
            org.slf4j.LoggerFactory.getLogger(this.getClass())
                    .info("Sending audio to OMR service for transcription");

            List<?> result = omrWebClient.post()
                    .uri("/analysis/audio")
                    .body(BodyInserters.fromMultipartData("file", new ByteArrayResource(audioBytes) {
                        @Override
                        public String getFilename() {
                            return filename;
                        }
                    }))
                    .retrieve()
                    .bodyToMono(List.class)
                    .block(Duration.ofSeconds(120));

            org.slf4j.LoggerFactory.getLogger(this.getClass())
                    .info("OMR service returned {} detected notes", result != null ? result.size() : 0);

            return result;
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(this.getClass())
                    .error("Error transcribing audio from OMR service", e);
            return null;
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

    private String extensionOf(String filename) {
        int idx = filename.lastIndexOf('.');
        return idx >= 0 ? filename.substring(idx) : "";
    }
}
