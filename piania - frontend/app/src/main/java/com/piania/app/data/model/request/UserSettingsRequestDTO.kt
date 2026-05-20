package com.piania.app.data.model.request

data class UserSettingsRequestDTO(
    val language: String,
    val notificationsEnabled: Boolean,
    val metronomeEnabled: Boolean,
    val defaultTempo: Int,
    val darkMode: Boolean,
    val cookiesAccepted: Boolean,
    val privacyPolicyAccepted: Boolean,
    val adsEnabled: Boolean
)
