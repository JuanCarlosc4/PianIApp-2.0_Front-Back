package com.piania.app.data.model.response


data class UserProfileResponseDTO(
    val id: String,
    val email: String,
    val fullName: String,
    val avatar: String,
    val role: String
)
