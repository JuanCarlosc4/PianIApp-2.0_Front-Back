package com.piania.app.data.model.response

import com.google.gson.annotations.SerializedName

data class ChatMessagePageResponseDTO(
    @SerializedName("content") val content: List<ChatMessageResponseDTO>,
    @SerializedName("totalElements") val totalElements: Long,
    @SerializedName("totalPages") val totalPages: Int,
    @SerializedName("number") val number: Int,
    @SerializedName("size") val size: Int
)
