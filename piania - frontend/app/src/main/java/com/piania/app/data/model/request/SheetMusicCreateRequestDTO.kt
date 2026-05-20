package com.piania.app.data.model.request
/**
 * Coincide con core-service: com.piania.core.dto.sheetmusic.SheetMusicRequest
 *
 * POST /piania/core/sheet-music
 */
data class SheetMusicCreateRequestDTO(
    val title: String,
    val composer: String? = null,
    val originalFileUrl: String,
    val isPublic: Boolean
)
