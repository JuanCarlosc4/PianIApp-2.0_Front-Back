package com.piania.app.data.model.response

import com.google.gson.annotations.SerializedName

data class ChatMessageResponseDTO(
    @SerializedName("id") val id: Long,
    @SerializedName("classId") val classId: Long,
    @SerializedName("senderEmail") val senderEmail: String,
    @SerializedName("senderName") val senderName: String? = null,
    @SerializedName("senderAvatar") val senderAvatar: String? = null,
    @SerializedName("senderIsTeacher") val senderIsTeacher: Boolean? = null,
    @SerializedName("pinned") val pinned: Boolean = false,
    @SerializedName("message") val message: String,
    @SerializedName("createdAt") val createdAt: String
)
