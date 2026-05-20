package com.piania.app.data.model.request

import java.util.Date

// Request para registrar una nueva práctica (POST /api/practicas)
data class PracticaRequestDTO(
    val idPartitura: Long,
    val duracion: Long, // Duración en milisegundos
    val archivoAudioPath: String?, // La ruta del archivo de audio subido o generado
    val fechaPractica: Date = Date()
)