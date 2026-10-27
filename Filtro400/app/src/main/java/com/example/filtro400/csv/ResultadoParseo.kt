package com.example.filtro400.csv

sealed class ResultadoLinea {
    abstract val numeroLineaOriginal: Int

    data class Ok(
        override val numeroLineaOriginal: Int,
        val fila: FilaCsv
    ) : ResultadoLinea()

    data class Error(
        override val numeroLineaOriginal: Int,
        val motivo: String
    ) : ResultadoLinea()
}

data class ResultadoParseo(
    val filas: List<FilaCsv>,
    val errores: List<ResultadoLinea.Error>
)
