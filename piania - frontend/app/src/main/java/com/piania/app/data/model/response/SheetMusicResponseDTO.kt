package com.piania.app.data.model.response

data class SheetMusicResponseDTO(
    val id: Long,
    val title: String,
    val composer: String?,
    val ownerEmail: String?,
    val musicXmlUrl: String?,
    val status: String?,
    val isPublic: Boolean = false,
    val tonalidad: String?,
    val compas: String?,
    // Backend nuevo: dificultadEstimada es Double
    val dificultadEstimada: Double?,
    val numeroCompases: Int?,
    // Backend nuevo: tempoDetectado
    val tempoDetectado: Int?,
    val analyzedAt: String?,
    val createdAt: String?
)
