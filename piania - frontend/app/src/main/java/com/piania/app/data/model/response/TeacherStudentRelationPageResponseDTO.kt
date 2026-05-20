package com.piania.app.data.model.response
import com.google.gson.annotations.SerializedName

data class TeacherStudentRelationPageResponseDTO(
    @SerializedName("content") val content: List<TeacherStudentRelationDTO> = emptyList(),
    @SerializedName("totalElements") val totalElements: Long = 0,
    @SerializedName("totalPages") val totalPages: Int = 0,
    @SerializedName("number") val number: Int = 0,
    @SerializedName("size") val size: Int = 0
)
