package com.piania.app.data.model.request

data class ChatReadStateUpsertRequestDTO(
    val virtualClassId: Long,
    val lastSeenMessageId: Long?
)
