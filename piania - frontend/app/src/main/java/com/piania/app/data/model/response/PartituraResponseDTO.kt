package com.piania.app.data.model.response

import com.google.gson.annotations.SerializedName

data class PartituraResponseDTO(
    @SerializedName("idPartitura")
    val idPartitura: Long,

    @SerializedName("titulo")
    val titulo: String,

    @SerializedName("fechaSubida")
    val fechaSubida: String?, // Usamos String para evitar problemas de parseo de fechas JSON

    @SerializedName("archivoOriginal")
    val archivoOriginal: String?,

    @SerializedName("archivoMusicXML")
    val archivoMusicXML: String?,

    @SerializedName("usuarioId")
    val usuarioId: Long?,

    @SerializedName("nombreUsuario")
    val nombreUsuario: String?,

    @SerializedName("analisis")
    val analisis: AnalisisResponseDTO?, // Puede ser nulo si aún no se ha analizado

    @SerializedName("procesada")
    val procesada: Boolean = false,

    @SerializedName("tieneError")
    val tieneError: Boolean = false
)