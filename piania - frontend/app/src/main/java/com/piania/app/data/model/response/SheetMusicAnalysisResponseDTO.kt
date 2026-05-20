package com.piania.app.data.model.response
import java.time.LocalDateTime

data class SheetMusicAnalysisResponseDTO(
    val tonalidad: String?,
    val compas: String?,
    val numeroCompases: Int?,
    val dificultadEstimada: Double?,
    val tempoDetectado: Int?,
    val fechaAnalisis: String?
)
