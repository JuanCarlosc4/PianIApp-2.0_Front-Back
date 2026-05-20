package com.piania.app.data.model.response

data class ShareLinkPageResponseDTO(
    val content: List<ShareLinkResponseDTO> = emptyList(),
    val totalElements: Long = 0,
    val totalPages: Int = 0,
    val number: Int = 0,
    val size: Int = 0
)
