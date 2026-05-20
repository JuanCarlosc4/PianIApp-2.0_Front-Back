package com.piania.app.data.model.request

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class ShareLinkCreateRequestDTO(
    val sheetMusicId: Long,
    val accessType: String, // e.g. PUBLIC / PRIVATE (según backend enum ShareAccessType)
    val expiresAt: String = defaultExpiresAtIso() // ISO-8601
) {
    companion object {
        /**
         * Backend valida expiresAt como obligatorio (aunque el DTO Java no tenga @NotNull),
         * así que mandamos por defecto +7 días si no se especifica.
         */
        fun defaultExpiresAtIso(days: Long = 7): String =
            LocalDateTime.now().plusDays(days).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    }
}
