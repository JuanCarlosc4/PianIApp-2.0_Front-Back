package com.piania.app.ui.partituras

import com.piania.app.R

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.piania.app.data.model.response.PartituraResponseDTO
import com.piania.app.data.repository.PartituraRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

@HiltViewModel
class RegistroPartituraViewModel @Inject constructor(
    application: Application,
    private val repository: PartituraRepository
) : AndroidViewModel(application) {

    private val _uploadState = MutableStateFlow<UploadState>(UploadState.Idle)
    val uploadState: StateFlow<UploadState> = _uploadState

    private val _archivoNombre = MutableStateFlow("")
    val archivoNombre: StateFlow<String> = _archivoNombre

    private var archivoSeleccionado: File? = null
    private var pollingJob: kotlinx.coroutines.Job? = null
    private var uploadedPartituraId: Long? = null

    fun setArchivoSeleccionado(file: File?) {
        archivoSeleccionado = file
        _archivoNombre.value = file?.name ?: ""
    }

    fun subirPartituraDesdeCompose(titulo: String) {
        archivoSeleccionado?.let { file ->
            subirPartitura(file, titulo)
        } ?: run {
            _uploadState.value = UploadState.Error(
                getApplication<Application>().getString(R.string.error_no_file_selected)
            )
        }
    }

    fun subirPartituraDesdeCompose(titulo: String, tempo: Int?) {
        archivoSeleccionado?.let { file ->
            subirPartitura(file, titulo, tempo)
        } ?: run {
            _uploadState.value = UploadState.Error(
                getApplication<Application>().getString(R.string.error_no_file_selected)
            )
        }
    }

    private fun subirPartitura(archivo: File, titulo: String, tempo: Int? = null) {
        if (titulo.isBlank()) {
            _uploadState.value = UploadState.Error(
                getApplication<Application>().getString(R.string.error_title_required)
            )
            return
        }

        viewModelScope.launch {
            _uploadState.value = UploadState.Loading

            try {
                val extension = archivo.extension.lowercase()
                val mimeType = when {
                    extension == "pdf" -> "application/pdf"
                    extension == "xml" || extension == "musicxml" -> "text/xml"
                    extension == "mxl" -> "application/vnd.recordare.musicxml+xml"
                    else -> "application/octet-stream"
                }

                val requestFile = archivo.asRequestBody(mimeType.toMediaTypeOrNull())
                val body = MultipartBody.Part.createFormData("file", archivo.name, requestFile)

                val createResult = repository.subirPartitura(body, titulo)
                val created = createResult.getOrElse { e ->
                    _uploadState.value = UploadState.Error(
                        e.message ?: getApplication<Application>()
                            .getString(R.string.error_unknown)
                    )
                    return@launch
                }

                uploadedPartituraId = created.idPartitura
                if (created.procesada) {
                    _uploadState.value = UploadState.Success(created)
                } else {
                    _uploadState.value = UploadState.Processing(created)
                    iniciarPolling(created.idPartitura)
                }
            } catch (e: Exception) {
                _uploadState.value = UploadState.Error(
                    getApplication<Application>()
                        .getString(R.string.error_preparing_upload) + ": ${e.message}"
                )
            }
        }
    }

    private fun iniciarPolling(partituraId: Long) {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            var intentos = 0
            val maxIntentos = 60
            while (intentos < maxIntentos) {
                delay(2000)
                intentos++

                val result = repository.getPartituraById(partituraId)
                val partitura = result.getOrNull()
                if (partitura != null) {
                    if (partitura.procesada) {
                        _uploadState.value = UploadState.Success(partitura)
                        pollingJob?.cancel()
                        return@launch
                    }
                }
            }
            _uploadState.value = UploadState.Error(
                getApplication<Application>()
                    .getString(R.string.error_processing_timeout)
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        pollingJob?.cancel()
    }

    sealed class UploadState {
        object Idle : UploadState()
        object Loading : UploadState()
        data class Processing(val data: PartituraResponseDTO) : UploadState()
        data class Success(val data: PartituraResponseDTO) : UploadState()
        data class Error(val message: String) : UploadState()
    }
}
