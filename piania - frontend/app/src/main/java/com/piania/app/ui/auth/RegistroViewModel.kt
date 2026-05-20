package com.piania.app.ui.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.piania.app.data.model.request.RegistroRequestDTO
import com.piania.app.data.model.response.AuthResponseDTO
import com.piania.app.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RegistroViewModel @Inject constructor(
    private val repository: AuthRepository
) : ViewModel() {

    // Devuelve un Result<Unit> para saber si fue exitoso o falló
    private val _registroResult = MutableLiveData<Result<AuthResponseDTO>>()
    val registroResult: LiveData<Result<AuthResponseDTO>> = _registroResult


    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading


    fun register(request: RegistroRequestDTO) {
        if (_isLoading.value == true) return

        _isLoading.value = true
        viewModelScope.launch {
            // Llama a la lógica de registro del AuthRepository
            val result = repository.register(request)
            _isLoading.value = false
            _registroResult.value = result
        }
    }
}
