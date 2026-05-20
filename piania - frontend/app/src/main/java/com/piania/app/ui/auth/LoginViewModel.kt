package com.piania.app.ui.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.piania.app.data.SessionManager
import com.piania.app.data.model.response.AuthResponseDTO
import com.piania.app.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val repository: AuthRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _isSessionActive = MutableLiveData<Boolean>()
    val isSessionActive: LiveData<Boolean> = _isSessionActive

    init {
        verificarSesionExistente()
    }

    private val _loginResult = MutableLiveData<Result<AuthResponseDTO>>()
    val loginResult: LiveData<Result<AuthResponseDTO>> = _loginResult

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    fun login(email: String, password: String) {
        _isLoading.value = true

        viewModelScope.launch {
            val result = repository.login(email, password)

            result.onSuccess { authResponse ->
                // El backend nuevo devuelve { accessToken, refreshToken }.
                sessionManager.saveUserSession(
                    token = authResponse.accessToken
                )

                _loginResult.value = result
                _isLoading.value = false
            }.onFailure { error ->
                // --- CORRECCIÓN ---
                // También debemos quitar el loading y notificar el error si falla
                _loginResult.value = Result.failure(error)
                _isLoading.value = false
            }
        }
    }

    private fun verificarSesionExistente() {
        viewModelScope.launch {
            sessionManager.authToken.collect { token ->
                if (!token.isNullOrEmpty()) {
                    _isSessionActive.value = true
                }
            }
        }
    }
}
