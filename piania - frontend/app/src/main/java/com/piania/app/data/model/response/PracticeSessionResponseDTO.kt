package com.piania.app.data.model.response

data class PracticeSessionResponseDTO(
    val id: Long,
    val sheetMusicId: Long,
    val audioUrl: String,
    val durationSeconds: Int,
    val score: Int?,
    val studentObservations: String?,
    val teacherCorrections: String?,
    val listenCount: Int?,
    val createdAt: String?
)
