package com.piania.app.ui.partituras

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.piania.app.data.model.response.AnalisisResponseDTO
import com.piania.app.data.model.response.FeedbackResponseDTO
import com.piania.app.data.SessionManager
import com.piania.app.data.repository.PartituraRepository
import com.piania.app.data.model.response.ShareLinkResponseDTO
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import javax.inject.Inject

typealias ShareLinkCallback = (ShareLinkResponseDTO) -> Unit

@HiltViewModel
class DetallePartituraViewModel @Inject constructor(
    application: Application,
    private val repository: PartituraRepository,
    private val sessionManager: SessionManager
    // private val userRepository: UserRepository // <--- Aquí inyectarías tu repositorio de usuarios
) : AndroidViewModel(application) {

    // --- ESTADOS DE LA PARTITURA (XML) ---
    var xmlContent by mutableStateOf<String?>(null)
    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)

    // --- ESTADO DEL FEEDBACK (AUDIO) ---
    var feedbackState by mutableStateOf<FeedbackState>(FeedbackState.Idle)
        private set

    // Premium eliminado

    // --- CARRIL 1: ESTADO DEL ANÁLISIS TEÓRICO (Music21) ---
    // Se usa SOLO para mostrar la ficha técnica (Tonalidad, BPM)
    var analisisTeorico by mutableStateOf<AnalisisResponseDTO?>(null)
        private set

    // --- CARRIL 2: ESTADO DE LA PRÁCTICA (AUDIO) ---
    // Se usa SOLO para mostrar la nota tras grabar (Precisión, Errores)
    // Usamos una Sealed Class pequeña aquí para controlar el spinner de "Subiendo audio..."
    var estadoPractica by mutableStateOf<EstadoPractica>(EstadoPractica.Idle)
        private set

    init {
        // Sin lógica premium
    }

    fun cargarPartitura(id: Long) {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null

            repository.getPartituraById(id)
                .onSuccess { partitura ->
                    val musicXmlUrl = partitura.archivoMusicXML

                    if (musicXmlUrl.isNullOrBlank()) {
                        errorMessage = "MusicXML no disponible todavía. Procesando..."
                        xmlContent = null
                        isLoading = false
                        return@onSuccess
                    }

                    // Si archivoMusicXML contiene ya el XML (legacy), lo detectamos heurísticamente.
                    if (musicXmlUrl.trimStart().startsWith("<")) {
                        xmlContent = musicXmlUrl
                        isLoading = false
                        return@onSuccess
                    }

                    // Nuevo backend: archivoMusicXML realmente es una URL (/uploads/..).
                    try {
                        val xml = loadMusicXmlWithCache(idPartitura = id, relativeOrAbsoluteUrl = musicXmlUrl)
                        xmlContent = xml
                    } catch (e: Exception) {
                        errorMessage = "Error cargando MusicXML: ${e.message}"
                        xmlContent = null
                    } finally {
                        isLoading = false
                    }
                }
                .onFailure { error ->
                    errorMessage = "Error al cargar: ${error.message}"
                    isLoading = false
                }
        }
    }

    private suspend fun loadMusicXmlWithCache(
        idPartitura: Long,
        relativeOrAbsoluteUrl: String
    ): String = withContext(Dispatchers.IO) {
        val context = getApplication<Application>()

        val cacheDir = File(context.filesDir, "sheet_music_cache/$idPartitura")
        cacheDir.mkdirs()
        val cachedFile = File(cacheDir, "score.musicxml")

        if (cachedFile.exists() && cachedFile.length() > 0) {
            return@withContext cachedFile.readText(Charsets.UTF_8)
        }

        // La URL del backend viene como "/uploads/...". Retrofit no sirve para ficheros estáticos fácilmente sin baseUrl.
        // Usamos OkHttp directo, reutilizando el mismo dominio que la app tenga en su config.
        // En este proyecto, la base URL suele apuntar al API Gateway; si relativeOrAbsoluteUrl es relativa, la convertimos.
        val baseUrl = repository.baseUrlForStaticResources()
        val finalUrl = if (relativeOrAbsoluteUrl.startsWith("http", ignoreCase = true)) {
            relativeOrAbsoluteUrl
        } else {
            baseUrl.trimEnd('/') + "/" + relativeOrAbsoluteUrl.trimStart('/')
        }

        val client = OkHttpClient()

        val token = sessionManager.fetchAuthToken()
        val requestBuilder = Request.Builder().url(finalUrl).get()
        if (!token.isNullOrBlank()) {
            requestBuilder.header("Authorization", "Bearer $token")
        }

        val request = requestBuilder.build()
        val response = client.newCall(request).execute()

        if (!response.isSuccessful) {
            throw IllegalStateException("HTTP ${response.code} al descargar MusicXML")
        }

        val body = response.body?.string() ?: throw IllegalStateException("Respuesta vacía")
        cachedFile.writeText(body, Charsets.UTF_8)
        body
    }

    fun subirGrabacion(idPartitura: Long, archivo: File) {
        viewModelScope.launch {
            estadoPractica = EstadoPractica.Cargando // Muestra spinner "Evaluando..."

            repository.subirAudioPractica(idPartitura, archivo)
                .onSuccess { feedback ->
                    estadoPractica = EstadoPractica.Exito(feedback) // Muestra nota
                }
                .onFailure { error ->
                    estadoPractica = EstadoPractica.Error(error.message ?: "Error al evaluar")
                }
        }
    }

    fun cargarAnalisisHistorico(idPartitura: Long) {
        viewModelScope.launch {

            feedbackState = FeedbackState.Enviando

            repository.getUltimoAnalisis(idPartitura)
                .onSuccess { analisis ->

                    if (analisis == null) {
                        feedbackState = FeedbackState.Vacio
                    } else {
                        feedbackState = FeedbackState.Exito(analisis)
                    }
                }
                .onFailure { error ->

                    Log.e(
                        "DetallePartituraVM",
                        "Error cargando análisis: ${error.message}",
                        error
                    )

                    feedbackState = FeedbackState.Vacio
                }
        }
    }

    fun resetFeedbackState() {
        feedbackState = FeedbackState.Idle
    }

    fun resetEstadoPractica() {
        estadoPractica = EstadoPractica.Idle
    }

    fun createShareLinkForSheetMusic(
        sheetMusicId: Long,
        accessType: String = "PUBLIC",
        onSuccess: ShareLinkCallback
    ) {
        viewModelScope.launch {
            runCatching {
                repository.createShareLink(sheetMusicId = sheetMusicId, accessType = accessType)
            }.onSuccess { result ->
                result.onSuccess { link ->
                    onSuccess(link)
                }.onFailure { err ->
                    Log.e("DetallePartituraVM", "Error creando share link: ${err.message}", err)
                }
            }.onFailure { e ->
                Log.e("DetallePartituraVM", "Error creando share link: ${e.message}", e)
            }
        }
    }
}

// Sealed Class para controlar la UI del análisis
sealed class FeedbackState {
    object Idle : FeedbackState()
    object Enviando : FeedbackState()
    object Vacio : FeedbackState()
    data class Exito(val analisis: AnalisisResponseDTO) : FeedbackState()
    data class Error(val mensaje: String) : FeedbackState()
}

sealed class EstadoPractica {
    object Idle : EstadoPractica()
    object Cargando : EstadoPractica()
    data class Exito(val feedback: FeedbackResponseDTO) : EstadoPractica()
    data class Error(val mensaje: String) : EstadoPractica()
}
