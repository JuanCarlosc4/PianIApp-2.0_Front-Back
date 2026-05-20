package com.piania.app.data.model.response

data class UserSettingsResponseDTO(
    val id: Long? = null,
    val userEmail: String? = null,
    val language: String,
    val notificationsEnabled: Boolean,
    val metronomeEnabled: Boolean,
    val defaultTempo: Int,
    val darkMode: Boolean,
    val cookiesAccepted: Boolean,
    val privacyPolicyAccepted: Boolean,
    val adsEnabled: Boolean,
    val createdAt: String? = null
)
