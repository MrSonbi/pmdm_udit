package com.example.filtro400.csv

/**
 * Textos que la pantalla Ajustes muestra tras importar un CSV.
 *
 * Función pura (sin Android) para poder probar las frases con JUnit:
 *  - resumen con añadidas / actualizadas / sin cambios (omitidas) / erróneas,
 *    con singular y plural correctos;
 *  - cada error con su número de línea; la línea resumen "y N más"
 *    (numeroLinea = 0, la crea [RecorteErrores]) se deja tal cual.
 */
object TextoImportacion {

    fun resumen(r: ImportadorCsv.ResultadoImportacion): String {
        if (r.fallo) {
            val causa = r.mensaje ?: "Error desconocido."
            return "Importación cancelada. $causa No se ha cambiado ninguna regla."
        }
        return "Importación completada: " +
            listOf(
                contar(r.anyadidas, "añadida", "añadidas"),
                contar(r.actualizadas, "actualizada", "actualizadas"),
                contar(r.omitidas, "omitida (sin cambios)", "omitidas (sin cambios)"),
                contar(r.erroneas, "errónea", "erróneas")
            ).joinToString(", ") + "."
    }

    fun linea(e: ImportadorCsv.ErrorLinea): String =
        if (e.numeroLinea > 0) "Línea ${e.numeroLinea}: ${e.mensaje}" else e.mensaje

    private fun contar(n: Int, singular: String, plural: String): String =
        if (n == 1) "1 $singular" else "$n $plural"
}
