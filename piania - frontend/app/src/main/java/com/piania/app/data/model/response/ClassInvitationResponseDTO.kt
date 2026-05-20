package com.piania.app.data.model.response

import com.google.gson.JsonElement

data class ClassInvitationResponseDTO(
    val token: String,
    val url: String,
    // Puede venir como ISO string o como array [year,month,day,...]
    val expiresAt: JsonElement?,
    val classId: Long
)
