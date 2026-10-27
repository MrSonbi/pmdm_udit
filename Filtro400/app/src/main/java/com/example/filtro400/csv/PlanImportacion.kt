package com.example.filtro400.csv

import com.example.filtro400.modelo.Regla

sealed class OperacionCsv {
    abstract val numeroLineaOriginal: Int
    abstract val motivo: String?

    data class Insertar(
        override val numeroLineaOriginal: Int,
        val patron: String,
        val etiqueta: String?,
        val tipo: com.example.filtro400.modelo.TipoRegla,
        val accion: com.example.filtro400.modelo.Accion,
        val prioridad: Int,
        val activa: Boolean,
        override val motivo: String? = null
    ) : OperacionCsv()

    data class Actualizar(
        override val numeroLineaOriginal: Int,
        val id: Long,
        val patron: String,
        val etiqueta: String?,
        val tipo: com.example.filtro400.modelo.TipoRegla,
        val accion: com.example.filtro400.modelo.Accion,
        val prioridad: Int,
        val activa: Boolean,
        override val motivo: String? = null
    ) : OperacionCsv()

    data class Omitir(
        override val numeroLineaOriginal: Int,
        override val motivo: String? = null
    ) : OperacionCsv()

    data class Error(
        override val numeroLineaOriginal: Int,
        override val motivo: String
    ) : OperacionCsv()
}

data class PlanImportacion(
    val operaciones: List<OperacionCsv>
)
