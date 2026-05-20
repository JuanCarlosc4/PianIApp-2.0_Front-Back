package com.piania.app.data.model.response

data class ClassEnrollmentResponseDTO(
    val id: Long,
    val virtualClassId: Long,
    val virtualClassName: String? = null,
    val studentEmail: String,
    val status: String? = null,
    val createdAt: String? = null
)
