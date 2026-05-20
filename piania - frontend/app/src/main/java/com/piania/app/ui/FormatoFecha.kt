package com.piania.app.ui

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale


class FormatoFecha {

    companion object{
        fun formatearFecha(fechaIso: String?): String {
            if (fechaIso.isNullOrBlank()) return ""

            return try {
                val entrada = LocalDateTime.parse(fechaIso)
                val salida = DateTimeFormatter.ofPattern("dd/MM/yyyy, HH:mm", Locale.getDefault())
                entrada.format(salida)
            } catch (e: Exception) {
                fechaIso
            }
        }
    }
}