package com.piania.app.data.model.response

import com.google.gson.annotations.SerializedName

data class JwtResponseDTO(
    @SerializedName("accessToken")
    val token: String,

    val type: String = "Bearer",
    val idUsuario: Long,
    val email: String,
    val nombre: String,
    val roles: List<String> = emptyList(),

)
