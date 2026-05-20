package com.piania.app.data.model.response

import com.google.gson.annotations.SerializedName

data class TeacherInvitationResponseDTO(
    @SerializedName("token") val token: String,
    @SerializedName("url") val url: String,
    // Puede venir como string ISO o como array [year,month,day,...] dependiendo del mapper del backend
    @SerializedName("expiresAt") val expiresAt: com.google.gson.JsonElement?
)
