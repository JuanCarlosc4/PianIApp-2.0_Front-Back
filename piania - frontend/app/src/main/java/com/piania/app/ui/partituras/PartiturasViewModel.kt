package com.piania.app.ui.partituras

import android.util.Log
import androidx.lifecycle.*
import com.piania.app.data.SessionManager
import com.piania.app.data.model.response.PartituraResponseDTO
import com.piania.app.data.repository.PartituraRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import retrofit2.HttpException
import javax.inject.Inject

@HiltViewModel
class PartiturasViewModel @Inject constructor(
    private val repository: PartituraRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _sessionExpired = MutableLiveData<Boolean>()
    val sessionExpired: LiveData<Boolean> = _sessionExpired

    private val _partituras = MutableLiveData<List<PartituraResponseDTO>>()
    val partituras: LiveData<List<PartituraResponseDTO>> = _partituras

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>(null)
    val errorMessage: LiveData<String?> = _errorMessage

    private var pollingJob: Job? = null

    fun loadPartituras() {
        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            refreshPartituras(startPollingIfNeeded = true)
        }
    }

    fun deletePartitura(id: Long) {
        viewModelScope.launch {
            // Placeholder: implement delete endpoint if needed
            loadPartituras()
        }
    }

    private suspend fun refreshPartituras(startPollingIfNeeded: Boolean) {
        val result = repository.getAllPartituras()
        _isLoading.value = false

        result.onSuccess { list ->
            Log.d("PartiturasVM", "Datos recibidos: ${list.size}")
            _partituras.value = list

            if (startPollingIfNeeded) {
                val hayPendientes = list.any { !it.procesada }
                if (hayPendientes) startPolling()
            }
        }

        result.onFailure { error ->
            if (error is HttpException && (error.code() == 401 || error.code() == 403)) {
                Log.e("PartiturasVM", "Token caducado. Cerrando sesión...")
                logout()
            } else {
                Log.e("PartiturasVM", "Error cargando: ${error.message}")
                _errorMessage.value = "Error de conexión: ${error.message}"
            }
            stopPolling()
        }
    }

    private fun startPolling() {
        if (pollingJob?.isActive == true) return

        pollingJob = viewModelScope.launch {
            while (isActive) {
                delay(15000)

                val result = repository.getAllPartituras()
                result.onSuccess { list ->
                    _partituras.postValue(list)

                    val hayPendientes = list.any { !it.procesada }
                    if (!hayPendientes) {
                        Log.d("PartiturasVM", "Todas procesadas. Fin del polling.")
                        stopPolling()
                        return@launch
                    }
                }.onFailure { error ->
                    if (error is HttpException && (error.code() == 401 || error.code() == 403)) {
                        logout()
                    } else {
                        _errorMessage.postValue("Error de conexión: ${error.message}")
                    }
                    stopPolling()
                    return@launch
                }
            }
        }
    }

    private fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    private fun logout() {
        viewModelScope.launch {
            sessionManager.clearSession()
            _sessionExpired.value = true
        }
    }

    override fun onCleared() {
        stopPolling()
        super.onCleared()
    }
}
