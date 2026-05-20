package com.piania.app.data.model.response
data class ClassEnrollmentPageResponseDTO(
    val content: List<ClassEnrollmentResponseDTO>,
    val totalElements: Long? = null,
    val totalPages: Int? = null,
    val number: Int? = null,
    val size: Int? = null
)
