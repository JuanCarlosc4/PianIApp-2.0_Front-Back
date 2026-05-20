package com.piania.app.data.model.response

import com.google.gson.annotations.SerializedName

data class AnnouncementResponseDTO(
    val id: Long,
    val title: String,
    // En backend nuevo el campo se llama "content" (no "message")
    @SerializedName("content")
    val message: String,
    @SerializedName("expiresAt")
    val expiresAt: String?,
    @SerializedName("active")
    val active: Boolean?,
    @SerializedName("createdAt")
    val createdAt: String? = null
)
