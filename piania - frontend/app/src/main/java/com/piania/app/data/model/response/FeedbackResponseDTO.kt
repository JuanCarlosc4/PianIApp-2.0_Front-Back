package com.piania.app.data.model.response

import com.google.gson.annotations.SerializedName
import java.util.Date

/**
 * Respuesta del backend nuevo: com.piania.core.dto.practicefeedback.FeedbackUploadResponse
 *
 * OJO: aunque el endpoint se llama /feedback/upload, el backend crea una PracticeSession y un PracticeFeedback.
 * Este DTO representa el "feedback" devuelto tras subir el audio.
 */
data class FeedbackResponseDTO(
    @SerializedName("practiceSessionId")
    val practiceSessionId: Long,

    @SerializedName("precisionGeneral")
    val precisionGeneral: Int, // 0..100

    @SerializedName("noteErrors")
    val noteErrors: Int,

    @SerializedName("rhythmErrors")
    val rhythmErrors: Int,

    @SerializedName("detailedReport")
    val detailedReport: String?,

    @SerializedName("createdAt")
    val createdAt: Date
) {
    // Compatibilidad mínima con nombres antiguos en UI legacy (si aún se usa).
    val idFeedback: Long get() = practiceSessionId
    val puntuacionGeneral: Int get() = precisionGeneral
    val erroresNota: Int get() = noteErrors
    val erroresRitmo: Int get() = rhythmErrors
    val comentarios: String? get() = detailedReport
    val fechaGeneracion: Date get() = createdAt
}
