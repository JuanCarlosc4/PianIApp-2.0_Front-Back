package com.piania.app.ui.chat

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.piania.app.data.model.response.ChatMessageResponseDTO
import com.piania.app.data.repository.ChatRepository
import com.piania.app.data.repository.PartituraRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import javax.inject.Inject
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import com.piania.app.R

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val partituraRepository: PartituraRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _messages = MutableLiveData<List<ChatMessageResponseDTO>>(emptyList())
    val messages: LiveData<List<ChatMessageResponseDTO>> = _messages

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    private val _lastSeenMessageId = MutableLiveData<Long?>(null)
    val lastSeenMessageId: LiveData<Long?> = _lastSeenMessageId

    private val _messageSentEvent = MutableLiveData<Boolean>(false)
    val messageSentEvent: LiveData<Boolean> = _messageSentEvent

    private val _importingSheetMusic = MutableLiveData(false)
    val importingSheetMusic: LiveData<Boolean> = _importingSheetMusic

    private val _importSheetMusicResult = MutableLiveData<String?>(null)
    val importSheetMusicResult: LiveData<String?> = _importSheetMusicResult

    private var currentClassId: Long? = null
    private var pollingJob: kotlinx.coroutines.Job? = null

    private fun showHttpError(prefix: String, code: Int, errorBody: String?) {
        _error.value = if (!errorBody.isNullOrBlank()) {
            "$prefix ($code): $errorBody"
        } else {
            "$prefix ($code)"
        }
    }

    fun loadMessages(classId: Long, page: Int = 0, size: Int = 50) {
        currentClassId = classId
        viewModelScope.launch {
            _isLoading.value = true

            val rs = chatRepository.getMyReadState(classId)
            if (rs.isSuccessful) {
                _lastSeenMessageId.value = rs.body()?.lastSeenMessageId
            }

            val response = chatRepository.listMessages(classId, page, size)
            if (response.isSuccessful) {
                val loaded = response.body()?.content ?: emptyList()
                _messages.value = loaded

                // ✅ Marcar automáticamente como leído el último mensaje al abrir el chat
                if (loaded.isNotEmpty()) {
                    val lastId = loaded.maxOf { it.id }
                    runCatching { chatRepository.upsertMyReadState(classId, lastId) }
                    _lastSeenMessageId.value = lastId
                }
            } else {
                val body = runCatching { response.errorBody()?.string() }.getOrNull()
                showHttpError(context.getString(R.string.error_loading_chat), response.code(), body)
            }

            _isLoading.value = false
            iniciarPolling(classId)
        }
    }

    private fun iniciarPolling(classId: Long) {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (true) {
                delay(3000)

                val response = chatRepository.listMessages(classId, page = 0, size = 50)
                if (response.isSuccessful) {
                    val newMessages = response.body()?.content ?: emptyList()
                    val currentMessages = _messages.value ?: emptyList()

                    if (newMessages.size > currentMessages.size ||
                        (newMessages.isNotEmpty() && currentMessages.isNotEmpty() &&
                         newMessages.last().id != currentMessages.last().id)) {
                        _messages.value = newMessages
                    }
                }
            }
        }
    }

    fun detenerPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    fun sendMessage(message: String) {
        val classId = currentClassId ?: run {
            _error.value = context.getString(R.string.error_class_not_selected)
            return
        }
        if (message.isBlank()) return

        viewModelScope.launch {
            _isLoading.value = true
            val response = chatRepository.sendMessage(classId, message.trim())
            if (response.isSuccessful) {
                val newMsg = response.body()
                if (newMsg != null) {
                    _messages.value = (_messages.value ?: emptyList()) + newMsg
                    _messageSentEvent.value = true
                    runCatching { chatRepository.upsertMyReadState(classId, newMsg.id) }
                    _lastSeenMessageId.value = newMsg.id
                } else {
                    loadMessages(classId)
                }
            } else {
                val body = runCatching { response.errorBody()?.string() }.getOrNull()
                showHttpError(context.getString(R.string.error_sending_message), response.code(), body)
            }
            _isLoading.value = false
        }
    }

    fun clearMessageSentEvent() {
        _messageSentEvent.value = false
    }

    fun clearImportSheetMusicResult() {
        _importSheetMusicResult.value = null
    }

    /**
     * Importa una partitura compartida a tu biblioteca a partir del token del share-link.
     * El repositorio:
     *  1) resuelve token -> sheetMusicId
     *  2) obtiene detalle de sheet music
     *  3) crea una copia del recurso en tu cuenta
     */
    fun importSheetMusicFromToken(token: String) {
        val cleanToken = token.trim()
        if (cleanToken.isBlank()) return

        viewModelScope.launch {
            _importingSheetMusic.value = true
            _importSheetMusicResult.value = null

            val result = partituraRepository.importSheetMusicFromShareToken(cleanToken)
            _importingSheetMusic.value = false

            result.fold(
                onSuccess = { p ->
                    _importSheetMusicResult.value =
                        context.getString(
                            R.string.sheet_music_saved,
                            p.titulo ?: context.getString(R.string.untitled)
                        )
                },
                onFailure = { e ->
                    _importSheetMusicResult.value =
                        e.message ?: context.getString(R.string.error_saving_sheet_music)
                }
            )
        }
    }

    fun pinMessage(messageId: Long) {
        val classId = currentClassId ?: run {
            _error.value = context.getString(R.string.error_class_not_selected)
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            val response = chatRepository.pinMessage(classId, messageId)
            if (response.isSuccessful) {
                loadMessages(classId)
            } else {
                val body = runCatching { response.errorBody()?.string() }.getOrNull()
                showHttpError(context.getString(R.string.error_pinning_message), response.code(), body)
            }
            _isLoading.value = false
        }
    }

    fun unpinMessage(messageId: Long) {
        val classId = currentClassId ?: run {
            _error.value = context.getString(R.string.error_class_not_selected)
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            val response = chatRepository.unpinMessage(classId, messageId)
            if (response.isSuccessful) {
                loadMessages(classId)
            } else {
                val body = runCatching { response.errorBody()?.string() }.getOrNull()
                showHttpError(context.getString(R.string.error_unpinning_message), response.code(), body)
            }
            _isLoading.value = false
        }
    }

    fun deleteMessage(messageId: Long) {
        val classId = currentClassId ?: run {
            _error.value = context.getString(R.string.error_class_not_selected)
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            val response = chatRepository.deleteMessage(classId, messageId)
            if (response.isSuccessful) {
                _messages.value = (_messages.value ?: emptyList()).filterNot { it.id == messageId }
                runCatching {
                    val rs = chatRepository.getMyReadState(classId)
                    if (rs.isSuccessful) _lastSeenMessageId.value = rs.body()?.lastSeenMessageId
                }
            } else {
                val body = runCatching { response.errorBody()?.string() }.getOrNull()
                showHttpError(context.getString(R.string.error_deleting_message), response.code(), body)
            }
            _isLoading.value = false
        }
    }

    fun clearError() {
        _error.value = null
    }

    override fun onCleared() {
        super.onCleared()
        detenerPolling()
    }
}
