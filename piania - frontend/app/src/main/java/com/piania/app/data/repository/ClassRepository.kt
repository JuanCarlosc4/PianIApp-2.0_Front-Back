package com.piania.app.data.repository

import com.google.gson.Gson
import com.piania.app.data.model.response.ClassInvitationResponseDTO
import com.piania.app.data.model.response.VirtualClassPageResponseDTO
import com.piania.app.data.remote.PianiaApiService
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

class ClassRepository @Inject constructor(
    private val apiService: PianiaApiService
) {
    suspend fun listEnrolledClasses(page: Int = 0, size: Int = 50): Result<VirtualClassPageResponseDTO> =
        safeCall { apiService.listEnrolledClasses(page = page, size = size) }

    suspend fun createClassInvitation(
        classId: Long,
        hoursValid: Int = 168
    ): Result<ClassInvitationResponseDTO> =
        safeCall { apiService.createClassInvitation(classId = classId, hoursValid = hoursValid) }

    suspend fun acceptClassInvitation(token: String): Result<Unit> =
        safeCall { apiService.acceptClassInvitation(token) }

    private suspend fun <T> safeCall(block: suspend () -> retrofit2.Response<T>): Result<T> {
        return try {
            val response = block()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(toDomainException(HttpException(response)))
            }
        } catch (e: IOException) {
            Result.failure(Exception("IOException: Error de red"))
        } catch (e: Exception) {
            val msg = e.message
            Result.failure(
                Exception(
                    if (!msg.isNullOrBlank()) "${e.javaClass.simpleName}: $msg" else e.javaClass.simpleName
                )
            )
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
