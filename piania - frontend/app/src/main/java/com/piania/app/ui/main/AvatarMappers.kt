package com.piania.app.ui.main

import com.piania.app.R

/**
 * Mapeos de valores del backend a recursos del frontend.
 *
 * - senderAvatar (usuario): "AVATAR_1".. "AVATAR_4"
 * - group avatar (clase): "GROUP_AVATAR_1".. "GROUP_AVATAR_4"
 *
 * Nota: por ahora no hay drawables dedicados, así que usamos iconos existentes
 * para evitar que la app reviente. Cuando existan recursos reales, sustituir aquí.
 */
fun userAvatarRes(avatar: String?): Int {
    return when (avatar) {
        "AVATAR_1" -> R.drawable.ic_sheet_music
        "AVATAR_2" -> R.drawable.ic_sheet_music
        "AVATAR_3" -> R.drawable.ic_sheet_music
        "AVATAR_4" -> R.drawable.ic_sheet_music
        else -> R.drawable.ic_sheet_music
    }
}

fun classGroupAvatarRes(index: Int): Int {
    return when (index) {
        1 -> R.drawable.ic_sheet_music
        2 -> R.drawable.ic_sheet_music
        3 -> R.drawable.ic_sheet_music
        4 -> R.drawable.ic_sheet_music
        else -> R.drawable.ic_sheet_music
    }
}

fun classGroupAvatarResFromEnum(groupAvatar: String?): Int {
    return when (groupAvatar?.trim()?.uppercase()) {
        "GROUP_AVATAR_1" -> classGroupAvatarRes(1)
        "GROUP_AVATAR_2" -> classGroupAvatarRes(2)
        "GROUP_AVATAR_3" -> classGroupAvatarRes(3)
        "GROUP_AVATAR_4" -> classGroupAvatarRes(4)
        else -> classGroupAvatarRes(1)
    }
}
