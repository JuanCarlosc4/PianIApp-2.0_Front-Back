package com.piania.app.data.repository

import com.piania.app.data.model.request.AnnouncementCreateRequestDTO
import com.piania.app.data.model.response.AnnouncementResponseDTO
import com.piania.app.data.remote.PianiaApiService
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

class AnnouncementRepository @Inject constructor(
    private val apiService: PianiaApiService
) {

    suspend fun listActive(page: Int = 0, size: Int = 20): Result<List<AnnouncementResponseDTO>> {
        return try {
            val response = apiService.listAnnouncements(page = page, size = size)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.content)
            } else {
                // Mejoramos el mensaje para el caso típico (401 -> token ausente/caducado)
                if (response.code() == 401) {
                    Result.failure(IllegalStateException("Sesión no válida. Inicia sesión de nuevo."))
                } else {
                    Result.failure(HttpException(response))
                }
            }
        } catch (e: IOException) {
            Result.failure(Exception("Error de red"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun create(
        title: String,
        message: String,
        expiresAt: java.util.Date? = null
    ): Result<AnnouncementResponseDTO> {
        return try {
            val response = apiService.createAnnouncement(
                AnnouncementCreateRequestDTO(
                    title = title,
                    message = message,
                    expiresAt = expiresAt
                )
            )

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(HttpException(response))
            }
        } catch (e: IOException) {
            Result.failure(Exception("Error de red"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deactivate(id: Long): Result<AnnouncementResponseDTO> {
        return try {
            val response = apiService.deactivateAnnouncement(id)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(HttpException(response))
            }
        } catch (e: IOException) {
            Result.failure(Exception("Error de red"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
