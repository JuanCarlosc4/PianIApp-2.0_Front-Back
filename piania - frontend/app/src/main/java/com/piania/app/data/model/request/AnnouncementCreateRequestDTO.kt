package com.piania.app.data.model.request

import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date

/**
 * Corresponde a com.piania.core.dto.announcement.AnnouncementRequest del backend.
 *
 * title: obligatorio
 * message: obligatorio
 * expiresAt: OBLIGATORIO (backend: @NotNull). Si falta -> HTTP 400 ("expiresAt no debe ser nulo")
 */
data class AnnouncementCreateRequestDTO(
    val title: String,
    val message: String,
    val expiresAt: Date? = defaultExpiresAtDate()
) {
    companion object {
        /**
         * Por defecto: +7 días.
         */
        fun defaultExpiresAtDate(days: Long = 7): Date {
            val ldt = LocalDateTime.now().plusDays(days)
            return Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant())
        }
    }
}
