package com.piania.app.data.model.response

import com.google.gson.annotations.SerializedName

/**
 * Representa el resumen del análisis musical devuelto por el servidor.
 * NOTA: Ya no contiene las notas individuales (Measure/Note),
 * sino los metadatos globales (Tonalidad, Tempo, Dificultad).
 */
data class AnalisisResponseDTO(
    @SerializedName("tonalidad")
    val tonalidad: String?,

    @SerializedName("compas")
    val compas: String?,

    @SerializedName("numeroCompases")
    val numeroCompases: Int,

    @SerializedName("dificultadEstimada")
    val dificultadEstimada: Double,

    @SerializedName("tempoDetectado")
    val tempoDetectado: Int?,

    @SerializedName("fechaAnalisis")
    val fechaAnalisis: String?
)
