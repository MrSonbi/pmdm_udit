package com.example.filtro400.csv

import com.example.filtro400.modelo.Regla

object ExportadorCsv {
    private const val BOM = "\uFEFF"

    fun exportar(reglas: List<Regla>): String {
        val sb = StringBuilder()
        sb.append(BOM)
        sb.append("patron;tipo;accion;prioridad;activa;etiqueta\n")
        for (regla in reglas) {
            val etiquetaEscapada = escapar(regla.etiqueta.orEmpty())
            val activa = if (regla.activa) "1" else "0"
            sb.append("${regla.patron};${regla.tipo.name};${regla.accion.name};${regla.prioridad};$activa;$etiquetaEscapada\n")
        }
        return sb.toString()
    }

    private fun escapar(valor: String): String {
        if (valor.contains(";") || valor.contains(",") || valor.contains("\"") || valor.contains("\n") || valor.contains("\r")) {
            return "\"" + valor.replace("\"", "\"\"") + "\""
        }
        return valor
    }
}
