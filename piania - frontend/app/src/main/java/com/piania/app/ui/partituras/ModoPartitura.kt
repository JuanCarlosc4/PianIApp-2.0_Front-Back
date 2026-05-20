package com.piania.app.ui.partituras

/**
 * Modo en el que se abre el detalle de una partitura.
 * Se usaba desde PartiturasScreen/DetallePartituraActivity vía Intent extra "MODO".
 */
enum class ModoPartitura {
    LECTURA,
    GRABACION,
    ANALISIS
}
