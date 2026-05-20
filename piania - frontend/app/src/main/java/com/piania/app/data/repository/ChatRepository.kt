package com.piania.app.data.repository

import com.piania.app.data.model.request.ChatMessageRequestDTO
import com.piania.app.data.model.request.ChatReadStateUpsertRequestDTO
import com.piania.app.data.model.response.ChatMessagePageResponseDTO
import com.piania.app.data.model.response.ChatMessageResponseDTO
import com.piania.app.data.model.response.ChatReadStateResponseDTO
import com.piania.app.data.remote.PianiaApiService
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val api: PianiaApiService
) {

    suspend fun listMessages(
        classId: Long,
        page: Int = 0,
        size: Int = 50
    ): Response<ChatMessagePageResponseDTO> = api.listChatMessages(classId, page, size)

    suspend fun sendMessage(
        classId: Long,
        message: String
    ): Response<ChatMessageResponseDTO> = api.sendChatMessage(classId, ChatMessageRequestDTO(message))

    suspend fun pinMessage(
        classId: Long,
        messageId: Long
    ): Response<Unit> = api.pinChatMessage(classId, messageId)

    suspend fun unpinMessage(
        classId: Long,
        messageId: Long
    ): Response<Unit> = api.unpinChatMessage(classId, messageId)

    suspend fun deleteMessage(
        classId: Long,
        messageId: Long
    ): Response<Unit> = api.deleteChatMessage(classId, messageId)

    suspend fun getMyReadState(
        virtualClassId: Long
    ): Response<ChatReadStateResponseDTO> = api.getMyChatReadState(virtualClassId)

    suspend fun upsertMyReadState(
        virtualClassId: Long,
        lastSeenMessageId: Long?
    ): Response<ChatReadStateResponseDTO> =
        api.upsertMyChatReadState(ChatReadStateUpsertRequestDTO(virtualClassId, lastSeenMessageId))
}
