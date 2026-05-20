package com.piania.app.ui.student

import android.app.Application
import androidx.lifecycle.*
import com.piania.app.R
import com.piania.app.data.model.response.VirtualClassDTO
import com.piania.app.data.repository.ClassRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StudentClassesViewModel @Inject constructor(
    application: Application,
    private val classRepository: ClassRepository
) : AndroidViewModel(application) {

    private val _classes = MutableLiveData<List<VirtualClassDTO>>(emptyList())
    val classes: LiveData<List<VirtualClassDTO>> = _classes

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    private fun string(resId: Int): String =
        getApplication<Application>().getString(resId)

    fun loadEnrolledClasses(page: Int = 0, size: Int = 50) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = classRepository.listEnrolledClasses(page, size)
            result.onSuccess { pageResponse ->
                _classes.value = pageResponse.content
            }.onFailure {
                _error.value =
                    it.message ?: string(R.string.error_loading_classes)
            }
            _isLoading.value = false
        }
    }

    fun clearError() {
        _error.value = null
    }
}
