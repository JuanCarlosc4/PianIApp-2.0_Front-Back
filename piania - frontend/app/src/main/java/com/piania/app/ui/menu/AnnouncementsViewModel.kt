package com.piania.app.ui.menu

import android.app.Application
import androidx.lifecycle.*
import com.piania.app.R
import com.piania.app.data.SessionManager
import com.piania.app.data.model.response.AnnouncementResponseDTO
import com.piania.app.data.repository.AnnouncementRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AnnouncementsViewModel @Inject constructor(
    application: Application,
    private val announcementRepository: AnnouncementRepository,
    private val sessionManager: SessionManager
) : AndroidViewModel(application) {

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _items = MutableLiveData<List<AnnouncementResponseDTO>>(emptyList())
    val items: LiveData<List<AnnouncementResponseDTO>> = _items

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    private val _hasUnread = MutableLiveData(false)
    val hasUnread: LiveData<Boolean> = _hasUnread

    private val _adminActionMessage = MutableLiveData<String?>(null)
    val adminActionMessage: LiveData<String?> = _adminActionMessage

    private fun string(resId: Int): String =
        getApplication<Application>().getString(resId)

    fun load(markAsSeen: Boolean = false) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            val result = announcementRepository.listActive(page = 0, size = 20)
            result.onSuccess { list ->
                _items.value = list

                val lastSeenId = sessionManager.getAnnouncementsLastSeenId()
                val maxId = list.maxOfOrNull { it.id } ?: 0L
                _hasUnread.value = maxId > lastSeenId

                if (markAsSeen && maxId > 0L) {
                    sessionManager.setAnnouncementsLastSeenId(maxId)
                    _hasUnread.value = false
                }
            }.onFailure { e ->
                _error.value =
                    e.message ?: string(R.string.error_loading_announcements)
            }

            _isLoading.value = false
        }
    }

    fun deactivateAnnouncement(id: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _adminActionMessage.value = null

            val result = announcementRepository.deactivate(id)
            result.onSuccess {
                _adminActionMessage.value =
                    string(R.string.announcement_deactivated)
                load(markAsSeen = false)
            }.onFailure {
                _error.value =
                    it.message
                        ?: string(R.string.error_deactivating_announcement)
            }

            _isLoading.value = false
        }
    }

    fun clearAdminMessage() {
        _adminActionMessage.value = null
    }
}
