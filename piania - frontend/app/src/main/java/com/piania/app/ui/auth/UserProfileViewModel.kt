package com.piania.app.ui.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.piania.app.data.model.response.UserProfileResponseDTO
import com.piania.app.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _profile = MutableLiveData<UserProfileResponseDTO?>()
    val profile: LiveData<UserProfileResponseDTO?> = _profile

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    private val _isUpdatingAvatar = MutableLiveData(false)
    val isUpdatingAvatar: LiveData<Boolean> = _isUpdatingAvatar

    fun loadMe() {
        _isLoading.value = true
        _error.value = null
        viewModelScope.launch {
            val result = authRepository.me()
            result.onSuccess { _profile.value = it }
            result.onFailure { _error.value = it.message }
            _isLoading.value = false
        }
    }

    fun updateAvatar(avatar: String) {
        _isUpdatingAvatar.value = true
        _error.value = null
        viewModelScope.launch {
            val result = authRepository.updateMyAvatar(avatar)
            result.onSuccess { _profile.value = it }
            result.onFailure { _error.value = it.message }
            _isUpdatingAvatar.value = false
        }
    }
}
