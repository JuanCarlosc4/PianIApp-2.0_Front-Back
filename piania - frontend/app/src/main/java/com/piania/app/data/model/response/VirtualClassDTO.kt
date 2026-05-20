package com.piania.app.data.model.response

data class VirtualClassDTO(
    val id: Long,
    val name: String,
    val teacherEmail: String,
    val groupAvatar: String? = null,
    val createdAt: String? = null
) {
    /**
     * Mapea el enum del backend (p.ej. GROUP_AVATAR_1) a un índice local.
     * El render lo hará el UI con recursos drawables propios.
     */
    fun groupAvatarIndex(): Int = when (groupAvatar?.trim()?.uppercase()) {
        "GROUP_AVATAR_1" -> 1
        "GROUP_AVATAR_2" -> 2
        "GROUP_AVATAR_3" -> 3
        "GROUP_AVATAR_4" -> 4
        else -> 1
    }
}
