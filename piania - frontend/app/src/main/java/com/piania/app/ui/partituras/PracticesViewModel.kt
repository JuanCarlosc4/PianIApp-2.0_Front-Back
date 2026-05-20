package com.piania.app.ui.partituras

import com.piania.app.R

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.piania.app.data.model.response.PracticeFeedbackResponseDTO
import com.piania.app.data.model.response.PracticeSessionResponseDTO
import com.piania.app.data.repository.PracticeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PracticesViewModel @Inject constructor(
    application: Application,
    private val practiceRepository: PracticeRepository
) : AndroidViewModel(application) {

    data class PracticeItemUi(
        val id: Long,
        val createdAt: String?,
        val score: Int?,
        val durationSeconds: Int,
        val studentObservations: String?,
        val teacherCorrections: String?
    )

    sealed class UiState {
        object Idle : UiState()
        object Loading : UiState()
        data class Error(val message: String) : UiState()
        data class Loaded(val items: List<PracticeItemUi>) : UiState()
    }

    sealed class DetailState {
        object Hidden : DetailState()
        object Loading : DetailState()
        data class Error(val message: String) : DetailState()
        data class Loaded(
            val practice: PracticeSessionResponseDTO,
            val feedback: PracticeFeedbackResponseDTO?
        ) : DetailState()
    }

    private val _state = MutableStateFlow<UiState>(UiState.Idle)
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val _detailState = MutableStateFlow<DetailState>(DetailState.Hidden)
    val detailState: StateFlow<DetailState> = _detailState.asStateFlow()

    private var currentSheetMusicId: Long? = null

    private val _navigateToHistory = MutableStateFlow<Long?>(null)
    val navigateToHistory: StateFlow<Long?> = _navigateToHistory.asStateFlow()

    fun selectSheetAndNavigate(sheetMusicId: Long) {
        _navigateToHistory.value = sheetMusicId
    }

    fun consumeNavigation() {
        _navigateToHistory.value = null
    }

    fun loadBySheetMusic(sheetMusicId: Long) {
        currentSheetMusicId = sheetMusicId
        _state.value = UiState.Loading

        viewModelScope.launch {
            val result = practiceRepository.listPracticesBySheetMusic(sheetMusicId, page = 0, size = 100)
            result.onSuccess { page ->
                val items = page.content.map { it.toUi() }
                _state.value = UiState.Loaded(items)
            }.onFailure { e ->
                _state.value = UiState.Error(
                    e.message ?: getApplication<Application>()
                        .getString(R.string.error_loading_practices)
                )
            }
        }
    }

    fun openPracticeDetail(practiceId: Long) {
        _detailState.value = DetailState.Loading

        viewModelScope.launch {
            val practiceResult = practiceRepository.getPractice(practiceId)
            practiceResult.onFailure { e ->
                _detailState.value = DetailState.Error(
                    e.message ?: getApplication<Application>()
                        .getString(R.string.error_loading_practice)
                )
                return@launch
            }

            val practice = practiceResult.getOrNull()!!

            // Feedback puede no existir todavía => no debe fallar la pantalla
            val feedbackResult = practiceRepository.getPracticeFeedback(practiceId)
            val feedback = feedbackResult.getOrNull()

            _detailState.value = DetailState.Loaded(practice = practice, feedback = feedback)
        }
    }

    fun closePracticeDetail() {
        _detailState.value = DetailState.Hidden
    }

    fun saveNotes(practiceId: Long, studentObservations: String?, teacherCorrections: String?) {
        // Optimista: dejamos el modal abierto y, si funciona, refrescamos detalle y listado
        viewModelScope.launch {
            val result = practiceRepository.updatePracticeNotes(practiceId, studentObservations, teacherCorrections)
            result.onSuccess {
                openPracticeDetail(practiceId)
                currentSheetMusicId?.let { loadBySheetMusic(it) }
            }.onFailure { e ->
                // Mantenemos el detalle pero marcamos error (para que la UI lo muestre)
                _detailState.value = DetailState.Error(
                    e.message ?: getApplication<Application>()
                        .getString(R.string.error_saving_changes)
                )
            }
        }
    }

    private fun PracticeSessionResponseDTO.toUi(): PracticeItemUi {
        return PracticeItemUi(
            id = id,
            createdAt = createdAt,
            score = score,
            durationSeconds = durationSeconds,
            studentObservations = studentObservations,
            teacherCorrections = teacherCorrections
        )
    }
}
