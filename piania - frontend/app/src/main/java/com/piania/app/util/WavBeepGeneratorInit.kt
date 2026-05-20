package com.piania.app.util

/**
 * Inicializador pequeño para no exponer internals del Metronome.
 * PianiaApplication llama a init(cacheDir) al arrancar.
 */
object WavBeepGeneratorInit {
    fun init(cacheDirPath: String) {
        // WavBeepGenerator es private en Metronome.kt; por eso este "bridge" vive en el mismo package.
        // Accedemos por reflexión de Kotlin (companion/object) NO; mejor: duplicamos el setter.
        // Como WavBeepGenerator es private, no es accesible desde aquí.
        //
        // Solución: mantenemos el estado global aquí y lo consume Metronome al crear el WAV.
        WavBeepCacheDirHolder.cacheDirPath = cacheDirPath
    }
}

/**
 * Holder de path compartido para generación del WAV del tick.
 */
internal object WavBeepCacheDirHolder {
    @Volatile var cacheDirPath: String? = null
}
