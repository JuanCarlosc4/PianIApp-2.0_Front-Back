package com.piania.app.data.repository

import com.google.gson.Gson
import com.piania.app.data.model.request.LoginRequestDTO
import com.piania.app.data.model.request.RegistroRequestDTO
import com.piania.app.data.model.response.AuthResponseDTO
import com.piania.app.data.model.response.UserProfileResponseDTO
import com.piania.app.data.remote.PianiaApiService
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

class AuthRepository @Inject constructor(
    private val apiService: PianiaApiService,
    private val gson: Gson
) {

    suspend fun login(email: String, password: String): Result<AuthResponseDTO> {
        return try {
            val response = apiService.login(LoginRequestDTO(email, password))

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(toApiException(response.code(), HttpException(response)))
            }
        } catch (e: IOException) {
            Result.failure(Exception("Error de red: Verifica tu conexión"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun register(registroRequest: RegistroRequestDTO): Result<AuthResponseDTO> {
        return try {
            val response = apiService.register(registroRequest)

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(toApiException(response.code(), HttpException(response)))
            }
        } catch (e: IOException) {
            Result.failure(Exception("Error de red al registrarse"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun me(): Result<UserProfileResponseDTO> {
        return try {
            val response = apiService.me()

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(toApiException(response.code(), HttpException(response)))
            }
        } catch (e: IOException) {
            Result.failure(Exception("Error de red al obtener perfil"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateMyAvatar(avatar: String): Result<UserProfileResponseDTO> {
        return try {
            val response = apiService.updateMyAvatar(avatar)

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(toApiException(response.code(), HttpException(response)))
            }
        } catch (e: IOException) {
            Result.failure(Exception("Error de red al actualizar avatar"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun toApiException(httpCode: Int, httpException: HttpException): Exception {
        val errorJson = try {
            httpException.response()?.errorBody()?.string()
        } catch (_: Exception) {
            null
        }

        if (errorJson.isNullOrBlank()) {
            return Exception("Error del servidor ($httpCode)")
        }

        return try {
            val apiError = gson.fromJson(errorJson, ApiErrorDTO::class.java)
            // apiError.message viene del backend (ej: "User not found")
            Exception(apiError.message.ifBlank { "Error del servidor (${apiError.status})" })
        } catch (_: Exception) {
            // fallback: si no es JSON parseable, mostramos el raw
            Exception("Error ($httpCode): $errorJson")
        }
    }
}
