package com.piania.app.data.model.response

data class ShareLinkResponseDTO(
    val id: Long,
    val token: String,
    val sheetMusicId: Long,
    val accessType: String,
    val expiresAt: String? = null,
    val revoked: Boolean = false,
    val createdAt: String? = null
)
