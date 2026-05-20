package com.piania.app.data.repository

import com.piania.app.data.model.request.SheetMusicCreateRequestDTO
import com.piania.app.data.model.response.AnalisisResponseDTO
import com.piania.app.data.model.response.FeedbackResponseDTO
import com.piania.app.data.model.response.PartituraResponseDTO
import com.piania.app.data.model.response.SheetMusicAnalysisResponseDTO
import com.piania.app.data.model.request.ShareLinkCreateRequestDTO
import com.piania.app.data.model.response.ShareLinkResponseDTO
import com.piania.app.data.model.response.SheetMusicResponseDTO
import com.google.gson.Gson
import com.piania.app.data.remote.PianiaApiService
import com.piania.app.data.repository.ApiErrorDTO
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.HttpException
import java.io.File
import javax.inject.Inject

class PartituraRepository @Inject constructor(
    private val apiService: PianiaApiService
) {

    suspend fun importSheetMusicFromShareToken(
        token: String
    ): Result<PartituraResponseDTO> {

        return try {

            val response = apiService.importSharedSheet(token)

            if (response.isSuccessful && response.body() != null) {

                Result.success(
                    response.body()!!.toLegacyPartitura()
                )

            } else {

                val raw = response.errorBody()?.string()?.trim().orEmpty()

                Result.failure(
                    Exception(
                        "Error ${response.code()}: ${
                            raw.ifBlank { "No se pudo importar la partitura" }
                        }"
                    )
                )
            }

        } catch (e: Exception) {

            Result.failure(e)
        }
    }

    // Para descargar recursos estáticos expuestos por el API Gateway (ej: /uploads/**).
    fun baseUrlForStaticResources(): String = com.piania.app.BuildConfig.BASE_URL

    // 1. OBTENER TODAS
    // Migración a backend nuevo: /api/sheet-music (core-service)
    suspend fun getAllPartituras(): Result<List<PartituraResponseDTO>> {
        return try {
            val response = apiService.listSheetMusic(page = 0, size = 200)
            if (response.isSuccessful && response.body() != null) {
                val page = response.body()!!
                val mapped = page.content.map { it.toLegacyPartitura() }
                Result.success(mapped)
            } else {
                Result.failure(HttpException(response))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun SheetMusicResponseDTO.toLegacyPartitura(): PartituraResponseDTO {
        val normalizedStatus = status?.uppercase()
        val isReady = normalizedStatus == "READY"

        return PartituraResponseDTO(
            idPartitura = id,
            titulo = title,
            fechaSubida = createdAt,
            archivoOriginal = null,
            archivoMusicXML = musicXmlUrl,
            usuarioId = null,
            nombreUsuario = ownerEmail,
            analisis = null,
            // En el backend nuevo, "READY" significa que ya hay MusicXML y metadatos calculados.
            // Mientras esté UPLOADED/PROCESSING, la UI debe considerarla "no procesada" y activar polling.
            procesada = isReady,
            // De momento el backend no expone un estado ERROR explícito: si hubiera, lo marcamos aquí.
            // Además, si la URL MusicXML es nula y no está en READY, seguimos sin error.
            tieneError = normalizedStatus == "ERROR",

        )
    }

    // 2. CREAR SHEET MUSIC (nuevo backend)
    // En el backend nuevo se sube primero el fichero a storage (obteniendo originalFileUrl)
    // y luego se crea el recurso sheet-music con ese URL.
    //
    // Por compatibilidad con la UI existente, mantenemos la firma "subirPartitura" pero
    // cambiamos internamente a POST /piania/core/sheet-music.
    suspend fun subirPartitura(
        filePart: MultipartBody.Part,
        titulo: String
    ): Result<PartituraResponseDTO> {
        return try {
            // 1) Subir fichero al core-service y obtener URL interna
            val normalizedFilePart = MultipartBody.Part.createFormData(
                "file",
                filePart.headers?.get("Content-Disposition")
                    ?.substringAfter("filename=\"")
                    ?.substringBefore("\"")
                    ?: "score.pdf",
                filePart.body
            )

            val uploadResponse = apiService.uploadFileToCore(normalizedFilePart)
            if (!uploadResponse.isSuccessful || uploadResponse.body() == null) {
                val errorMsg = uploadResponse.errorBody()?.string() ?: "Error desconocido"
                return Result.failure(Exception("Error ${uploadResponse.code()}: $errorMsg"))
            }
            val uploaded = uploadResponse.body()!!

            // 2) Crear SheetMusic referenciando el fichero subido
            val createResponse = apiService.createSheetMusic(
                SheetMusicCreateRequestDTO(
                    title = titulo,
                    composer = null,
                    originalFileUrl = uploaded.url,
                    isPublic = false
                )
            )

            if (!createResponse.isSuccessful || createResponse.body() == null) {
                val errorMsg = createResponse.errorBody()?.string() ?: "Error desconocido"
                return Result.failure(Exception("Error ${createResponse.code()}: $errorMsg"))
            }

            val created = createResponse.body()!!

            // 3) Lanzar procesado (MusicXML + análisis)
            // El backend encolará el procesado automáticamente. No hacemos llamada explícita.
            // Devolvemos el estado creado que estará en UPLOADED/PROCESSING
            Result.success(created.toLegacyPartitura())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 3. DETALLE
    // En backend nuevo no hay "legacy /partituras/{id}".
    // Para tener detalle + refresco de estado, tiramos de listSheetMusic y filtramos.
    suspend fun getPartituraById(id: Long): Result<PartituraResponseDTO> {
        return try {
            val response = apiService.listSheetMusic(page = 0, size = 200)
            if (response.isSuccessful && response.body() != null) {
                val page = response.body()!!
                val found = page.content.firstOrNull { it.id == id }
                    ?: return Result.failure(Exception("Partitura no encontrada"))
                Result.success(found.toLegacyPartitura())
            } else {
                Result.failure(HttpException(response))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 4. SUBIR AUDIO PRACTICA (nuevo flujo backend)
    suspend fun subirAudioPractica(id: Long, file: File): Result<FeedbackResponseDTO> {
        return try {
            val extension = file.extension.lowercase()
            val mimeType = when (extension) {
                "3gp" -> "audio/3gpp"
                "m4a" -> "audio/mp4"
                "wav" -> "audio/wav"
                else -> "audio/*"
            }

            // 1️⃣ Subir audio al core-service
            val requestFile = file.asRequestBody(mimeType.toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("file", file.name, requestFile)

            val uploadResponse = apiService.uploadFileToCore(body)
            if (!uploadResponse.isSuccessful || uploadResponse.body() == null) {
                val errorMsg = uploadResponse.errorBody()?.string() ?: "Error subiendo audio"
                return Result.failure(Exception("Error ${uploadResponse.code()}: $errorMsg"))
            }

            val audioUrl = uploadResponse.body()!!.url

            // 2️⃣ Crear PracticeSession (backend genera automáticamente PracticeFeedback)
            val createResponse = apiService.createPractice(
                mapOf(
                    "sheetMusicId" to id,
                    "audioUrl" to audioUrl,
                    "durationSeconds" to 5
                )
            )

            if (!createResponse.isSuccessful || createResponse.body() == null) {
                val errorMsg = createResponse.errorBody()?.string() ?: "Error creando práctica"
                return Result.failure(Exception("Error ${createResponse.code()}: $errorMsg"))
            }

            val practiceId = createResponse.body()!!.id

            // 3️⃣ Polling hasta que el backend genere el feedback (OMR puede tardar unos segundos)
            var feedbackBody: Any? = null
            val maxAttempts = 10
            val delayMillis = 1500L

            repeat(maxAttempts) { attempt ->
                val feedbackResponse = apiService.getPracticeFeedback(practiceId)

                if (feedbackResponse.isSuccessful && feedbackResponse.body() != null) {
                    feedbackBody = feedbackResponse.body()
                    return@repeat
                }

                if (attempt < maxAttempts - 1) {
                    kotlinx.coroutines.delay(delayMillis)
                }
            }

            if (feedbackBody == null) {
                return Result.failure(
                    Exception("El análisis está tardando más de lo esperado. Inténtalo en unos segundos.")
                )
            }

            // Convertimos PracticeFeedbackResponseDTO → FeedbackResponseDTO legacy
            val feedbackLegacy = Gson().fromJson(
                Gson().toJson(feedbackBody),
                FeedbackResponseDTO::class.java
            )

            Result.success(feedbackLegacy)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun SheetMusicAnalysisResponseDTO.toLegacyAnalisis(): AnalisisResponseDTO {
        return AnalisisResponseDTO(
            tonalidad = tonalidad,
            compas = compas,
            numeroCompases = numeroCompases ?: 0,
            dificultadEstimada = dificultadEstimada ?: 0.0,
            tempoDetectado = tempoDetectado,
            fechaAnalisis = fechaAnalisis?.toString()
        )
    }

    // 5. OBTENER ÚLTIMO ANÁLISIS (nuevo backend)
    suspend fun getUltimoAnalisis(id: Long): Result<AnalisisResponseDTO> {
        return try {
            val response = apiService.getSheetMusicAnalysis(id)

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.toLegacyAnalisis())
            } else {
                Result.failure(Exception("Error ${response.code()}: No se pudo obtener el análisis."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 6. SHARE LINKS (nuevo backend)
    suspend fun createShareLink(sheetMusicId: Long, accessType: String = "PUBLIC"): Result<ShareLinkResponseDTO> {
        return try {
            val response = apiService.createShareLink(
                ShareLinkCreateRequestDTO(
                    sheetMusicId = sheetMusicId,
                    accessType = accessType
                )
            )

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val raw = response.errorBody()?.string()?.trim().orEmpty()
                Result.failure(Exception("Error ${response.code()}: ${raw.ifBlank { "No se pudo crear el enlace" }}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
