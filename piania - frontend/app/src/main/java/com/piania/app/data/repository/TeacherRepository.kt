package com.piania.app.data.repository


import com.piania.app.data.model.request.ClassEnrollmentRequestDTO
import com.piania.app.data.model.request.ShareLinkCreateRequestDTO
import com.piania.app.data.model.response.ClassEnrollmentResponseDTO
import com.piania.app.data.model.response.ShareLinkResponseDTO
import com.piania.app.data.model.response.TeacherInvitationResponseDTO
import com.piania.app.data.model.response.TeacherStudentRelationDTO
import com.piania.app.data.model.response.VirtualClassDTO
import com.piania.app.data.model.response.VirtualClassPageResponseDTO
import com.google.gson.Gson
import com.piania.app.data.remote.PianiaApiService
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject


class TeacherRepository @Inject constructor(
    private val apiService: PianiaApiService
) {

    suspend fun createClass(name: String): Result<VirtualClassDTO> =
        safeCall { apiService.createClass(name) }

    suspend fun listMyClasses(page: Int = 0, size: Int = 50): Result<VirtualClassPageResponseDTO> =
        safeCall { apiService.listMyClasses(page = page, size = size) }

    suspend fun deleteClass(id: Long): Result<Unit> =
        safeCallUnit { apiService.deleteClass(id) }

    suspend fun updateClass(id: Long, name: String?, groupAvatar: String?): Result<VirtualClassDTO> =
        safeCall { apiService.updateClass(id = id, name = name, groupAvatar = groupAvatar) }

    suspend fun linkToTeacher(teacherEmail: String): Result<Unit> =
        safeCallUnit { apiService.linkToTeacher(teacherEmail) }

    suspend fun listMyStudents(page: Int = 0, size: Int = 200): Result<List<TeacherStudentRelationDTO>> =
        safeCall { apiService.listMyStudents(page = page, size = size) }.map { it.content }

    suspend fun addStudentToClass(classId: Long, studentEmail: String): Result<ClassEnrollmentResponseDTO> =
        safeCall { apiService.addStudentToClass(ClassEnrollmentRequestDTO(classId = classId, studentEmail = studentEmail)) }

    suspend fun listClassStudents(classId: Long): Result<List<ClassEnrollmentResponseDTO>> =
        safeCall { apiService.listClassStudents(classId) }.map { it.content }

    suspend fun removeStudentFromClass(enrollmentId: Long): Result<Unit> =
        safeCallUnit { apiService.removeStudentFromClass(enrollmentId) }

    suspend fun createShareLink(body: ShareLinkCreateRequestDTO): Result<Unit> =
        safeCall { apiService.createShareLink(body) }.map { Unit }

    suspend fun listMyShareLinks(page: Int = 0, size: Int = 200): Result<List<ShareLinkResponseDTO>> =
        safeCall { apiService.listMyShareLinks(page = page, size = size) }.map { it.content }

    suspend fun revokeShareLink(id: Long): Result<Unit> =
        safeCallUnit { apiService.revokeShareLink(id) }

    private suspend fun <T> safeCall(block: suspend () -> retrofit2.Response<T>): Result<T> {
        return try {
            val response = block()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(toDomainException(HttpException(response)))
            }
        } catch (e: IOException) {
            Result.failure(Exception("Error de red"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun safeCallUnit(block: suspend () -> retrofit2.Response<*>): Result<Unit> {
        return try {
            val response = block()
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(toDomainException(HttpException(response)))
            }
        } catch (e: IOException) {
            Result.failure(Exception("Error de red"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun toDomainException(http: HttpException): Exception {
        return try {
            val errorBody = http.response()?.errorBody()?.string()
            if (!errorBody.isNullOrBlank()) {
                val apiError = Gson().fromJson(errorBody, ApiErrorDTO::class.java)
                if (!apiError.message.isNullOrBlank()) Exception(apiError.message) else http
            } else {
                http
            }
        } catch (_: Exception) {
            http
        }
    }
}
