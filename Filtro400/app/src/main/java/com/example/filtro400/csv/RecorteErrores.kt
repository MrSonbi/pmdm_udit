package com.example.filtro400.csv

/**
 * Recorte de la lista de errores de una importación. Es el ÚNICO sitio donde
 * se recorta: [com.example.filtro400.data.RepositorioReglas.aplicarPlan]
 * devuelve todos los errores y [ImportadorCsv.importarDesdeStream] llama aquí
 * una sola vez, con los errores de parseo y los del plan ya juntos.
 *
 * Función pura (sin Android) para poder probarla en JVM.
 */
object RecorteErrores {

    /** Máximo de errores que se devuelven con su número de línea. */
    const val MAXIMO = 50

    /**
     * Devuelve los primeros [maximo] errores de [todos] y, si sobran, añade al
     * final una línea resumen (`numeroLinea = 0`) "y N más", con
     * N = total - maximo calculado sobre la lista COMPLETA.
     */
    fun recortar(
        todos: List<ImportadorCsv.ErrorLinea>,
        maximo: Int = MAXIMO
    ): List<ImportadorCsv.ErrorLinea> {
        if (todos.size <= maximo) return todos
        val restantes = todos.size - maximo
        return todos.take(maximo) + ImportadorCsv.ErrorLinea(0, "y $restantes más")
    }
}
