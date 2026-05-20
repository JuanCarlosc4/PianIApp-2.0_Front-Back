package com.piania.app.data.model.response
import com.google.gson.annotations.SerializedName

data class AuthResponseDTO(
    @SerializedName("accessToken")
    val accessToken: String,
    @SerializedName("refreshToken")
    val refreshToken: String
)
