package com.piania.app.data.model.response

data class PracticeSessionPageResponseDTO(
    val content: List<PracticeSessionResponseDTO> = emptyList(),
    val totalPages: Int = 0,
    val totalElements: Long = 0,
    val last: Boolean = true,
    val size: Int = 0,
    val number: Int = 0,
    val numberOfElements: Int = 0,
    val first: Boolean = true,
    val empty: Boolean = true
)
