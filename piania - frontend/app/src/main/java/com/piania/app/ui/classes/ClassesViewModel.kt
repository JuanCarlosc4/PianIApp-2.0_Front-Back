package com.piania.app.ui.classes

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.piania.app.data.JwtUtils
import com.piania.app.data.SessionManager
import com.piania.app.data.model.response.VirtualClassDTO
import com.piania.app.data.repository.ChatRepository
import com.piania.app.data.repository.ClassRepository
import com.piania.app.data.repository.TeacherRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ClassesViewModel @Inject constructor(
    private val classRepository: ClassRepository,
    private val teacherRepository: TeacherRepository,
    private val chatRepository: ChatRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _classes = MutableLiveData<List<VirtualClassDTO>>(emptyList())
    val classes: LiveData<List<VirtualClassDTO>> = _classes

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    private val _unreadCounts = MutableLiveData<Map<Long, Int>>(emptyMap())
    val unreadCounts: LiveData<Map<Long, Int>> = _unreadCounts

    fun loadClasses(isTeacher: Boolean, page: Int = 0, size: Int = 50) {
        viewModelScope.launch {
            _isLoading.value = true

            val result = if (isTeacher) {
                teacherRepository.listMyClasses(page = page, size = size)
                    .map { it.content }
            } else {
                classRepository.listEnrolledClasses(page = page, size = size)
                    .map { it.content }
            }

            result.onSuccess { list ->
                _classes.value = list
                loadUnreadCounts(list)
            }.onFailure {
                val msg = it.message
                _error.value = if (!msg.isNullOrBlank()) {
                    "${it.javaClass.simpleName}: $msg"
                } else {
                    "Unexpected error (${it.javaClass.simpleName})"
                }
            }

            _isLoading.value = false
        }
    }

    fun loadUnreadCounts(classes: List<VirtualClassDTO>, size: Int = 50) {
        viewModelScope.launch {
            val counts = mutableMapOf<Long, Int>()
            val myEmail = JwtUtils.getEmail(sessionManager.fetchAuthToken())

            for (c in classes) {
                val classId = c.id

                val lastSeenId = runCatching {
                    val rs = chatRepository.getMyReadState(classId)
                    if (rs.isSuccessful) rs.body()?.lastSeenMessageId else null
                }.getOrNull()

                val unreadCount = runCatching {
                    val pageResp = chatRepository.listMessages(classId = classId, page = 0, size = size)
                    if (!pageResp.isSuccessful) return@runCatching 0
                    val msgs = pageResp.body()?.content.orEmpty()
                    msgs.count { message ->
                        (lastSeenId == null || message.id > lastSeenId) &&
                            !message.senderEmail.equals(myEmail, ignoreCase = true)
                    }
                }.getOrNull() ?: 0

                counts[classId] = unreadCount
            }

            _unreadCounts.value = counts
        }
    }

    fun clearError() {
        _error.value = null
    }
}
