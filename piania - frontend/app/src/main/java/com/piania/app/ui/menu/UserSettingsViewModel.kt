package com.piania.app.ui.menu

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.piania.app.data.model.request.UserSettingsRequestDTO
import com.piania.app.data.model.response.UserSettingsResponseDTO
import com.piania.app.data.repository.UserSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserSettingsViewModel @Inject constructor(
    private val repository: UserSettingsRepository
) : ViewModel() {

    private val _settings = MutableLiveData<UserSettingsResponseDTO?>()
    val settings: LiveData<UserSettingsResponseDTO?> = _settings

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    fun load() {
        _isLoading.value = true
        _error.value = null
        viewModelScope.launch {
            try {
                _settings.value = repository.getUserSettings()
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun save(body: UserSettingsRequestDTO) {
        _isLoading.value = true
        _error.value = null
        viewModelScope.launch {
            try {
                _settings.value = repository.upsertUserSettings(body)
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }
}
