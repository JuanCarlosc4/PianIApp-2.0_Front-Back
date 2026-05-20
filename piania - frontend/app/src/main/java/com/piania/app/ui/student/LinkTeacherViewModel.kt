package com.piania.app.ui.student

import android.app.Application
import androidx.lifecycle.*
import com.piania.app.R
import com.piania.app.data.repository.TeacherRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LinkTeacherViewModel @Inject constructor(
    application: Application,
    private val teacherRepository: TeacherRepository
) : AndroidViewModel(application) {

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _message = MutableLiveData<String?>(null)
    val message: LiveData<String?> = _message

    private fun string(resId: Int): String =
        getApplication<Application>().getString(resId)

    fun clearMessage() {
        _message.value = null
    }

    fun linkToTeacher(teacherEmail: String) {
        val normalized = teacherEmail.trim()
        if (normalized.isBlank()) {
            _message.value = string(R.string.email_required)
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            val result = teacherRepository.linkToTeacher(normalized)
            result.onSuccess {
                _message.value =
                    string(R.string.link_with_teacher_desc)
            }.onFailure { e ->
                _message.value =
                    e.message ?: string(R.string.error_link_teacher)
            }
            _isLoading.value = false
        }
    }
}
