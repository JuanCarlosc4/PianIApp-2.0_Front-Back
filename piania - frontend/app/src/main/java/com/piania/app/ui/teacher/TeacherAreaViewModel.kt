package com.piania.app.ui.teacher

import android.app.Application
import androidx.lifecycle.*
import com.piania.app.R
import com.piania.app.data.model.response.ClassEnrollmentResponseDTO
import com.piania.app.data.model.response.ShareLinkResponseDTO
import com.piania.app.data.model.response.TeacherStudentRelationDTO
import com.piania.app.data.model.response.VirtualClassDTO
import com.piania.app.data.model.response.VirtualClassPageResponseDTO
import com.piania.app.data.repository.TeacherRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TeacherAreaViewModel @Inject constructor(
    application: Application,
    private val teacherRepository: TeacherRepository
) : AndroidViewModel(application) {

    private val _classesPage = MutableLiveData<VirtualClassPageResponseDTO?>(null)
    val classesPage: LiveData<VirtualClassPageResponseDTO?> = _classesPage

    private val _classes = MutableLiveData<List<VirtualClassDTO>>(emptyList())
    val classes: LiveData<List<VirtualClassDTO>> = _classes

    private val _selectedClassStudents =
        MutableLiveData<List<ClassEnrollmentResponseDTO>>(emptyList())
    val selectedClassStudents: LiveData<List<ClassEnrollmentResponseDTO>> =
        _selectedClassStudents

    private val _shareLinks =
        MutableLiveData<List<ShareLinkResponseDTO>>(emptyList())
    val shareLinks: LiveData<List<ShareLinkResponseDTO>> = _shareLinks

    private val _myStudents =
        MutableLiveData<List<TeacherStudentRelationDTO>>(emptyList())
    val myStudents: LiveData<List<TeacherStudentRelationDTO>> = _myStudents

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    private fun string(resId: Int): String =
        getApplication<Application>().getString(resId)

    private fun setError(msg: String?) {
        _error.value = msg
    }

    fun refreshAll() {
        loadClasses()
        loadMyStudents()
    }

    fun loadClasses(page: Int = 0, size: Int = 50) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = teacherRepository.listMyClasses(page = page, size = size)
            result.onSuccess { pageResponse ->
                _classesPage.value = pageResponse
                _classes.value = pageResponse.content
            }.onFailure {
                setError(it.message ?: string(R.string.error_loading_classes))
            }
            _isLoading.value = false
        }
    }

    fun createClass(name: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = teacherRepository.createClass(name)
            result.onSuccess { loadClasses() }
                .onFailure {
                    setError(it.message ?: string(R.string.error_creating_class))
                }
            _isLoading.value = false
        }
    }

    fun deleteClass(id: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = teacherRepository.deleteClass(id)
            result.onSuccess { loadClasses() }
                .onFailure {
                    setError(it.message ?: string(R.string.error_deleting_class))
                }
            _isLoading.value = false
        }
    }

    fun updateClass(id: Long, name: String?, groupAvatar: String?) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = teacherRepository.updateClass(id, name, groupAvatar)
            result.onSuccess { loadClasses() }
                .onFailure {
                    setError(it.message ?: string(R.string.error_updating_class))
                }
            _isLoading.value = false
        }
    }

    fun loadClassStudents(classId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = teacherRepository.listClassStudents(classId)
            result.onSuccess { _selectedClassStudents.value = it }
                .onFailure {
                    setError(
                        it.message
                            ?: string(R.string.error_loading_class_students)
                    )
                }
            _isLoading.value = false
        }
    }

    fun addStudentToClass(classId: Long, email: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = teacherRepository.addStudentToClass(classId, email)
            result.onSuccess { loadClassStudents(classId) }
                .onFailure {
                    setError(it.message ?: string(R.string.error_adding_student))
                }
            _isLoading.value = false
        }
    }

    fun removeStudentFromClass(classId: Long, enrollmentId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = teacherRepository.removeStudentFromClass(enrollmentId)
            result.onSuccess { loadClassStudents(classId) }
                .onFailure {
                    setError(it.message ?: string(R.string.error_removing_student))
                }
            _isLoading.value = false
        }
    }

    fun loadShareLinks() {
        viewModelScope.launch {
            _isLoading.value = true
            val result = teacherRepository.listMyShareLinks()
            result.onSuccess { _shareLinks.value = it }
                .onFailure {
                    setError(it.message ?: string(R.string.error_loading_links))
                }
            _isLoading.value = false
        }
    }

    fun createShareLink(sheetMusicId: Long, accessType: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = teacherRepository.createShareLink(
                com.piania.app.data.model.request.ShareLinkCreateRequestDTO(
                    sheetMusicId = sheetMusicId,
                    accessType = accessType
                )
            )
            result.onSuccess { loadShareLinks() }
                .onFailure {
                    setError(it.message ?: string(R.string.error_creating_link))
                }
            _isLoading.value = false
        }
    }

    fun revokeShareLink(id: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = teacherRepository.revokeShareLink(id)
            result.onSuccess { loadShareLinks() }
                .onFailure {
                    setError(it.message ?: string(R.string.error_revoking_link))
                }
            _isLoading.value = false
        }
    }

    fun loadMyStudents() {
        viewModelScope.launch {
            _isLoading.value = true
            val result = teacherRepository.listMyStudents()
            result.onSuccess { _myStudents.value = it }
                .onFailure {
                    setError(
                        it.message
                            ?: string(R.string.error_loading_my_students)
                    )
                }
            _isLoading.value = false
        }
    }

    fun clearError() = setError(null)
}
