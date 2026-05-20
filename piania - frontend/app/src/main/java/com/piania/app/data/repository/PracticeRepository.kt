package com.piania.app.data.repository

import com.piania.app.data.model.request.PracticeSessionUpdateRequestDTO
import com.piania.app.data.model.response.PracticeFeedbackResponseDTO
import com.piania.app.data.model.response.PracticeSessionPageResponseDTO
import com.piania.app.data.model.response.PracticeSessionResponseDTO
import com.piania.app.data.remote.PianiaApiService
import retrofit2.HttpException
import javax.inject.Inject

class PracticeRepository @Inject constructor(
    private val apiService: PianiaApiService
) {
    suspend fun listPracticesBySheetMusic(
        sheetMusicId: Long,
        page: Int = 0,
        size: Int = 50
    ): Result<PracticeSessionPageResponseDTO> {
        return try {
            val response = apiService.listPracticesBySheetMusic(sheetMusicId, page, size)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(HttpException(response))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPractice(practiceId: Long): Result<PracticeSessionResponseDTO> {
        return try {
            val response = apiService.getPractice(practiceId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(HttpException(response))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updatePracticeNotes(
        practiceId: Long,
        studentObservations: String?,
        teacherCorrections: String?
    ): Result<PracticeSessionResponseDTO> {
        return try {
            val response = apiService.updatePracticeNotes(
                practiceId,
                PracticeSessionUpdateRequestDTO(
                    studentObservations = studentObservations,
                    teacherCorrections = teacherCorrections
                )
            )
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(HttpException(response))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPracticeFeedback(practiceId: Long): Result<PracticeFeedbackResponseDTO> {
        return try {
            val response = apiService.getPracticeFeedback(practiceId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(HttpException(response))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
