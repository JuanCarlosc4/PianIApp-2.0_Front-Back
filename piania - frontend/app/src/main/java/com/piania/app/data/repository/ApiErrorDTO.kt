package com.piania.app.data.repository
import com.google.gson.annotations.SerializedName

data class ApiErrorDTO(
    @SerializedName("status") val status: Int = 0,
    @SerializedName("message") val message: String = "",
    // backend manda LocalDateTime como string, lo dejamos como String para no pelear con formatos
    @SerializedName("timestamp") val timestamp: String? = null
)
