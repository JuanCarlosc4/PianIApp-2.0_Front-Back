package com.piania.app.data.model.response

import com.google.gson.annotations.SerializedName

data class PracticaResponseDTO(
    @SerializedName("idPractica")
    val idPractica: Long,

    @SerializedName("fechaPractica")
    val fechaPractica: String?,

    @SerializedName("duracion")
    val duracion: Long,

    @SerializedName("entradaAudio")
    val archivoAudioPath: String?,

    // Relaciones Aplanadas (IDs)
    @SerializedName("idPartitura")
    val idPartitura: Long,

    @SerializedName("idUsuario")
    val idUsuario: Long,

    @SerializedName("nombreUsuario")
    val nombreUsuario: String?,

    // Objeto anidado (Feedback)
    @SerializedName("feedback")
    val feedback: FeedbackResponseDTO?
)