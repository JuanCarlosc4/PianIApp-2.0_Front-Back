package com.piania.app.data.model.response

import com.google.gson.annotations.SerializedName

/**
 * Respuesta paginada estándar de Spring Data para /classes/my.
 *
 * NOTA: el backend devuelve un objeto con campos como: content, totalElements, totalPages, number, size...
 */
data class VirtualClassPageResponseDTO(
    @SerializedName("content") val content: List<VirtualClassDTO> = emptyList(),
    @SerializedName("totalElements") val totalElements: Long = 0,
    @SerializedName("totalPages") val totalPages: Int = 0,
    @SerializedName("number") val number: Int = 0,
    @SerializedName("size") val size: Int = 0,
    @SerializedName("first") val first: Boolean = false,
    @SerializedName("last") val last: Boolean = false
)
