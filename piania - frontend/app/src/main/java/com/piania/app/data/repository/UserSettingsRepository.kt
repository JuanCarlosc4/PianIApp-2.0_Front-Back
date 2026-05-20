package com.piania.app.data.repository

import com.piania.app.data.model.request.UserSettingsRequestDTO
import com.piania.app.data.model.response.UserSettingsResponseDTO
import com.piania.app.data.remote.PianiaApiService
import javax.inject.Inject

class UserSettingsRepository @Inject constructor(
    private val apiService: PianiaApiService
) {
    suspend fun getUserSettings(): UserSettingsResponseDTO {
        val resp = apiService.getUserSettings()
        if (!resp.isSuccessful || resp.body() == null) {
            throw Exception("Error cargando ajustes (${resp.code()})")
        }
        return resp.body()!!
    }

    suspend fun upsertUserSettings(body: UserSettingsRequestDTO): UserSettingsResponseDTO {
        val resp = apiService.upsertUserSettings(body)
        if (!resp.isSuccessful || resp.body() == null) {
            throw Exception("Error guardando ajustes (${resp.code()})")
        }
        return resp.body()!!
    }
}
