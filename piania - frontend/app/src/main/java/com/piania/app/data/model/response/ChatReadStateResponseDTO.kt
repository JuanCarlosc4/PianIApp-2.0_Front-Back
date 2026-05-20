package com.piania.app.data.model.response
/**
 * Estado de lectura del chat por usuario y clase.
 *
 * Se usa para marcar qué último mensaje ha "visto" el usuario.
 */
data class ChatReadStateResponseDTO(
    val virtualClassId: Long,
    val userEmail: String,
    val lastSeenMessageId: Long?
)
