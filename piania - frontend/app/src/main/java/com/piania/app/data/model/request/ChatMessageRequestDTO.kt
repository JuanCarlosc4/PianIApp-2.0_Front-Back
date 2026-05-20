package com.piania.app.data.model.request

import com.google.gson.annotations.SerializedName

data class ChatMessageRequestDTO(
    @SerializedName("message") val message: String
)
