package com.piania.core.service;

import com.piania.core.service.PracticeEvaluationService;
import com.piania.core.dto.practice.PracticeSessionRequest;
import com.piania.core.entity.PracticeFeedback;
import com.piania.core.entity.PracticeSession;
import com.piania.core.entity.SheetMusic;
import com.piania.core.entity.UserProgress;
import com.piania.core.exception.BadRequestException;
import com.piania.core.exception.ForbiddenException;
import com.piania.core.exception.NotFoundException;
import com.piania.core.repository.PracticeFeedbackRepository;
import com.piania.core.repository.PracticeSessionRepository;
import com.piania.core.repository.SheetMusicRepository;
import com.piania.core.repository.UserProgressRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.Map;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PracticeService {

    private final PracticeSessionRepository practiceSessionRepository;
    private final PracticeFeedbackRepository practiceFeedbackRepository;
    private final SheetMusicRepository sheetMusicRepository;
    private final UserProgressRepository userProgressRepository;
    private final SubscriptionService subscriptionService;
    private final WebClient omrWebClient;
    private final PracticeEvaluationService practiceEvaluationService;

    public PracticeSession createPractice(String userEmail, PracticeSessionRequest request) {

        SheetMusic sheetMusic = sheetMusicRepository.findById(request.getSheetMusicId())
                .orElseThrow(() -> new NotFoundException("Sheet music not found"));

        if (!sheetMusic.getOwnerEmail().equals(userEmail) && !sheetMusic.isPublic()) {
            throw new ForbiddenException("You cannot practice this sheet music");
        }

        subscriptionService.validateWeeklyPracticeLimitForSheetMusic(userEmail, sheetMusic);

        int realDuration = calculateAudioDurationSeconds(request.getAudioUrl());

        PracticeSession session = PracticeSession.builder()
                .userEmail(userEmail)
                .sheetMusic(sheetMusic)
                .audioUrl(request.getAudioUrl())
                .durationSeconds(realDuration)
                .score(0)
                .listenCount(0)
                .deleted(false)
                .build();

        PracticeSession saved = practiceSessionRepository.save(session);

        // Generate automatic feedback asynchronously (non-blocking)
        generateFeedbackAsync(saved.getId());

        return saved;
    }

    public PracticeSession createPracticeFromUpload(String userEmail,
                                                    Long sheetMusicId,
                                                    String audioUrl,
                                                    Integer durationSeconds) {
        return createPractice(
                userEmail,
                PracticeSessionRequest.builder()
                        .sheetMusicId(sheetMusicId)
                        .audioUrl(audioUrl)
                        .durationSeconds(durationSeconds != null && durationSeconds > 0 ? durationSeconds : 1)
                        .build()
        );
    }

    public Page<PracticeSession> getUserPractices(String userEmail, Pageable pageable) {
        return practiceSessionRepository.findByUserEmailAndDeletedFalse(userEmail, pageable);
    }

    public Page<PracticeSession> getPracticesBySheetMusic(Long sheetMusicId, String requesterEmail, Pageable pageable) {
        SheetMusic sheetMusic = sheetMusicRepository.findById(sheetMusicId)
                .orElseThrow(() -> new NotFoundException("Sheet music not found"));

        // Owner puede ver; si es pública, cualquiera autenticado puede ver; si no, sólo owner.
        if (!sheetMusic.getOwnerEmail().equals(requesterEmail) && !sheetMusic.isPublic()) {
            throw new ForbiddenException("Access denied");
        }

        return practiceSessionRepository.findBySheetMusicAndDeletedFalse(sheetMusic, pageable);
    }

    public PracticeSession getPracticeById(Long id, String userEmail) {

        PracticeSession session = practiceSessionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Practice not found"));

        if (!session.getUserEmail().equals(userEmail)) {
            throw new ForbiddenException("Access denied");
        }

        return session;
    }

    public PracticeSession updatePracticeNotes(Long practiceId,
                                               String requesterEmail,
                                               boolean requesterIsTeacher,
                                               String studentObservations,
                                               String teacherCorrections) {

        PracticeSession session = practiceSessionRepository.findById(practiceId)
                .orElseThrow(() -> new NotFoundException("Practice not found"));

        // El alumno propietario siempre puede escribir sus observaciones.
        if (studentObservations != null) {
            if (!session.getUserEmail().equals(requesterEmail)) {
                throw new ForbiddenException("Access denied");
            }
            session.setStudentObservations(studentObservations);
        }

        // Las correcciones las escribe un teacher (o admin) sobre una práctica del alumno.
        if (teacherCorrections != null) {
            if (!requesterIsTeacher) {
                throw new ForbiddenException("Access denied");
            }
            session.setTeacherCorrections(teacherCorrections);
        }

        return practiceSessionRepository.save(session);
    }

    public PracticeFeedback getPracticeFeedback(Long practiceSessionId,
                                                String requesterEmail,
                                                boolean requesterIsTeacher) {

        PracticeSession session = practiceSessionRepository.findById(practiceSessionId)
                .orElseThrow(() -> new NotFoundException("Practice not found"));

        // El alumno propietario puede ver su feedback, y el teacher (o admin) también.
        if (!session.getUserEmail().equals(requesterEmail) && !requesterIsTeacher) {
            throw new ForbiddenException("Access denied");
        }

        return practiceFeedbackRepository.findByPracticeSessionId(practiceSessionId)
                .orElseThrow(() -> new NotFoundException("Practice feedback not found"));
    }


    @org.springframework.scheduling.annotation.Async
    public void generateFeedbackAsync(Long practiceId) {
        try {
            PracticeSession session = practiceSessionRepository.findById(practiceId)
                    .orElseThrow(() -> new NotFoundException("Practice not found"));

            log.info("Calling OMR service asynchronously for practice {}", session.getId());

            byte[] audioBytes = java.nio.file.Files.readAllBytes(
                resolveUploadsPath(session.getAudioUrl())
            );

            Object response = omrWebClient.post()
                    .uri("/analysis/audio")
                    .body(org.springframework.web.reactive.function.BodyInserters
                            .fromMultipartData("file",
                                    new org.springframework.core.io.ByteArrayResource(audioBytes) {
                                        @Override
                                        public String getFilename() {
                                            return "practice.wav";
                                        }
                                    }))
                    .retrieve()
                    .bodyToMono(List.class)
                    .block(Duration.ofSeconds(120));

            if (!(response instanceof List<?> notesList)) {
                log.warn("OMR returned null or invalid response for practice {}", session.getId());
                return;
            }

            List<PracticeEvaluationService.DetectedNote> detectedNotes = notesList.stream()
                    .filter(Map.class::isInstance)
                    .map(Map.class::cast)
                    .map(this::mapToDetectedNote)
                    .toList();

            PracticeEvaluationService.EvaluationResult evaluation =
                    practiceEvaluationService.evaluate(session.getSheetMusic(), detectedNotes);

            PracticeFeedback feedback = PracticeFeedback.builder()
                    .practiceSession(session)
                    .precisionGeneral(evaluation.getPrecisionGeneral())
                    .noteErrors(evaluation.getNoteErrors())
                    .rhythmErrors(evaluation.getRhythmErrors())
                    .detailedReport(evaluation.getDetailedReport())
                    .build();

            practiceFeedbackRepository.save(feedback);

            session.setScore(evaluation.getPrecisionGeneral());
            practiceSessionRepository.save(session);

            updateUserProgress(
                    session.getUserEmail(),
                    session.getSheetMusic(),
                    session.getDurationSeconds(),
                    evaluation.getPrecisionGeneral()
            );

            log.info("Asynchronous feedback saved for practice {}", session.getId());

        } catch (Exception ex) {
            log.error("OMR ASYNC ERROR for practice {}: {}", practiceId, ex.getMessage(), ex);

            practiceSessionRepository.findById(practiceId).ifPresent(s -> {
                s.setScore(-1);
                practiceSessionRepository.save(s);
            });
        }
    }


    private int calculateAudioDurationSeconds(String audioUrl) {
        try {
            java.nio.file.Path path = java.nio.file.Paths.get("uploads")
                    .resolve(audioUrl.replace("/uploads/", ""));

            javax.sound.sampled.AudioInputStream audioInputStream =
                    javax.sound.sampled.AudioSystem.getAudioInputStream(path.toFile());

            javax.sound.sampled.AudioFormat format = audioInputStream.getFormat();
            long frames = audioInputStream.getFrameLength();
            double durationInSeconds = (frames + 0.0) / format.getFrameRate();

            return (int) Math.round(durationInSeconds);
        } catch (Exception e) {
            log.warn("Unable to calculate audio duration, defaulting to 1 second", e);
            return 1;
        }
    }

    private void updateUserProgress(String userEmail,
                                    SheetMusic sheetMusic,
                                    int duration,
                                    int score) {

        UserProgress progress = userProgressRepository
                .findByUserEmailAndSheetMusic(userEmail, sheetMusic)
                .orElse(UserProgress.builder()
                        .userEmail(userEmail)
                        .sheetMusic(sheetMusic)
                        .totalPractices(0)
                        .averageScore(0)
                        .totalPracticeTimeSeconds(0)
                        .build());

        int newTotalPractices = progress.getTotalPractices() + 1;
        int newTotalTime = progress.getTotalPracticeTimeSeconds() + duration;

        int newAverage = ((progress.getAverageScore() * progress.getTotalPractices()) + score)
                / newTotalPractices;

        progress.setTotalPractices(newTotalPractices);
        progress.setTotalPracticeTimeSeconds(newTotalTime);
        progress.setAverageScore(newAverage);

        userProgressRepository.save(progress);
    }

    private PracticeEvaluationService.DetectedNote mapToDetectedNote(Map<?, ?> raw) {
        double timestamp = toDouble(firstNonNull(
                raw.get("timestamp"),
                raw.get("start"),
                raw.get("time"),
                raw.get("onset"),
                raw.get("start_time")
        ));

        int midiNote = toInt(firstNonNull(
                raw.get("midiNote"),
                raw.get("pitch"),
                raw.get("note"),
                raw.get("midi_note"),
                raw.get("value")
        ));

        double duration = toDouble(firstNonNull(
                raw.get("duration"),
                raw.get("dur"),
                raw.get("length")
        ));

        return new PracticeEvaluationService.DetectedNote(timestamp, midiNote, duration);
    }

    private Object firstNonNull(Object... values) {
        for (Object value : values) {
            if (value != null) return value;
        }
        return null;
    }

    private double toDouble(Object value) {
        if (value == null) return 0d;
        if (value instanceof Number n) return n.doubleValue();
        return Double.parseDouble(value.toString());
    }

    private int toInt(Object value) {
        if (value == null) return 0;
        if (value instanceof Number n) return n.intValue();
        return Integer.parseInt(value.toString());
    }

    private java.nio.file.Path resolveUploadsPath(String fileUrl) {
        String normalized = fileUrl == null ? "" : fileUrl.trim().replace("\\", "/");

        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }

        if (normalized.startsWith("uploads/")) {
            normalized = normalized.substring("uploads/".length());
        }

        return java.nio.file.Paths.get("uploads", normalized);
    }

}
