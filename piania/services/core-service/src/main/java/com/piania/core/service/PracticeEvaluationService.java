package com.piania.core.service;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.piania.core.entity.SheetMusic;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PracticeEvaluationService {

    private final SheetMusicParsingService sheetMusicParsingService;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DetectedNote {
        @JsonAlias({"start", "time", "onset", "start_time", "timestamp"})
        private double timestamp;

        @JsonAlias({"pitch", "note", "midi_note", "value", "midiNote"})
        private int midiNote;

        @JsonAlias({"dur", "length", "duration"})
        private double duration;
    }

    @Data
    @AllArgsConstructor
    public static class EvaluationResult {
        private int precisionGeneral;
        private int noteErrors;
        private int rhythmErrors;
        private String detailedReport;
    }

    public EvaluationResult evaluate(SheetMusic sheetMusic, List<DetectedNote> detectedNotes) {
        List<Integer> expectedNotes = sheetMusicParsingService.extractExpectedMidiNotes(sheetMusic);

        if (expectedNotes.isEmpty()) {
            return new EvaluationResult(
                    0,
                    0,
                    0,
                    "No se pudieron extraer notas esperadas desde el MusicXML."
            );
        }

        if (detectedNotes == null || detectedNotes.isEmpty()) {
            return new EvaluationResult(
                    0,
                    expectedNotes.size(),
                    expectedNotes.size(),
                    "No se detectaron notas en el audio."
            );
        }

        List<DetectedNote> cleaned = limpiarArmonicos(detectedNotes);
        List<DetectedNote> fused = fusionarNotasFragmentadas(cleaned);
        List<DetectedNote> synced = sincronizarInicio(fused, expectedNotes);

        List<Integer> detectedMidi = synced.stream()
                .map(DetectedNote::getMidiNote)
                .collect(Collectors.toList());

        int distance = calculateLevenshteinDistance(expectedNotes, detectedMidi);
        int maxLength = Math.max(expectedNotes.size(), detectedMidi.size());

        int precision = maxLength > 0
                ? Math.max(0, Math.min(100, Math.round((1f - ((float) distance / maxLength)) * 100f)))
                : 0;

        int noteErrors = distance;
        int rhythmErrors = estimateRhythmErrors(expectedNotes, synced);

        String report = "Esperadas: " + expectedNotes.size()
                + ", detectadas: " + detectedMidi.size()
                + ", tras limpieza: " + cleaned.size()
                + ", tras fusión: " + fused.size()
                + ", precisión: " + precision + "%"
                + ", errores de nota: " + noteErrors
                + ", errores de ritmo: " + rhythmErrors + ".";

        return new EvaluationResult(
                precision,
                noteErrors,
                rhythmErrors,
                report
        );
    }

    private List<DetectedNote> limpiarArmonicos(List<DetectedNote> entrada) {
        if (entrada == null || entrada.isEmpty()) return new ArrayList<>();

        List<DetectedNote> resultado = new ArrayList<>();
        boolean[] ignorar = new boolean[entrada.size()];
        double toleranciaTiempo = 0.10;

        for (int i = 0; i < entrada.size(); i++) {
            if (ignorar[i]) continue;
            DetectedNote base = entrada.get(i);

            for (int j = i + 1; j < entrada.size(); j++) {
                if (ignorar[j]) continue;
                DetectedNote comp = entrada.get(j);

                if (Math.abs(comp.getTimestamp() - base.getTimestamp()) > toleranciaTiempo) break;

                int dif = Math.abs(comp.getMidiNote() - base.getMidiNote());

                if (dif == 12 || dif == 19 || dif == 24) {
                    if (comp.getMidiNote() > base.getMidiNote()) {
                        ignorar[j] = true;
                    } else {
                        ignorar[i] = true;
                        break;
                    }
                }
            }
        }

        for (int i = 0; i < entrada.size(); i++) {
            if (!ignorar[i]) resultado.add(entrada.get(i));
        }

        return resultado;
    }

    private List<DetectedNote> fusionarNotasFragmentadas(List<DetectedNote> entrada) {
        if (entrada == null || entrada.isEmpty()) return new ArrayList<>();

        List<DetectedNote> salida = new ArrayList<>();
        DetectedNote actual = new DetectedNote(
                entrada.get(0).getTimestamp(),
                entrada.get(0).getMidiNote(),
                entrada.get(0).getDuration()
        );

        for (int i = 1; i < entrada.size(); i++) {
            DetectedNote siguiente = entrada.get(i);
            double silencioEntreNotas = siguiente.getTimestamp() - (actual.getTimestamp() + actual.getDuration());

            if (siguiente.getMidiNote() == actual.getMidiNote() && silencioEntreNotas < 0.15) {
                double nuevoFin = siguiente.getTimestamp() + siguiente.getDuration();
                actual.setDuration(nuevoFin - actual.getTimestamp());
            } else {
                salida.add(actual);
                actual = new DetectedNote(
                        siguiente.getTimestamp(),
                        siguiente.getMidiNote(),
                        siguiente.getDuration()
                );
            }
        }

        salida.add(actual);
        return salida;
    }

    private List<DetectedNote> sincronizarInicio(List<DetectedNote> audio, List<Integer> expectedNotes) {
        if (audio == null || audio.size() < 3 || expectedNotes == null || expectedNotes.size() < 3) {
            return audio != null ? audio : new ArrayList<>();
        }

        List<Integer> pattern = expectedNotes.stream().limit(3).collect(Collectors.toList());

        int startIndex = -1;
        for (int i = 0; i <= audio.size() - 3; i++) {
            if (matches(audio.get(i).getMidiNote(), pattern.get(0))
                    && matches(audio.get(i + 1).getMidiNote(), pattern.get(1))
                    && matches(audio.get(i + 2).getMidiNote(), pattern.get(2))) {
                startIndex = i;
                break;
            }
        }

        if (startIndex > 0) {
            return new ArrayList<>(audio.subList(startIndex, audio.size()));
        }

        return audio;
    }

    private int estimateRhythmErrors(List<Integer> expectedNotes, List<DetectedNote> synced) {
        // Primera versión simple: dejamos 0 hasta modelar duraciones esperadas del XML
        return 0;
    }

    private boolean matches(int a, int b) {
        return Math.abs(a - b) <= 1;
    }

    private int calculateLevenshteinDistance(List<Integer> x, List<Integer> y) {
        int[][] dp = new int[x.size() + 1][y.size() + 1];

        for (int i = 0; i <= x.size(); i++) {
            for (int j = 0; j <= y.size(); j++) {
                if (i == 0) {
                    dp[i][j] = j;
                } else if (j == 0) {
                    dp[i][j] = i;
                } else {
                    boolean coinciden = Math.abs(x.get(i - 1) - y.get(j - 1)) <= 1;
                    int cost = coinciden ? 0 : 1;

                    dp[i][j] = Math.min(
                            Math.min(dp[i - 1][j - 1] + cost, dp[i - 1][j] + 1),
                            dp[i][j - 1] + 1
                    );
                }
            }
        }

        return dp[x.size()][y.size()];
    }
}
