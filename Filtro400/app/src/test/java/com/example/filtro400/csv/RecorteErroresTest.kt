package com.example.filtro400.csv

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * El recorte de errores se hace UNA vez y "y N más" se calcula sobre el total.
 */
class RecorteErroresTest {

    /** Genera [n] errores con números de línea 1..n. */
    private fun errores(n: Int) = (1..n).map { ImportadorCsv.ErrorLinea(it, "error $it") }

    @Test
    fun ceroErroresDevuelveListaVacia() {
        assertTrue(RecorteErrores.recortar(errores(0)).isEmpty())
    }

    @Test
    fun cincuentaErroresSeDevuelvenTodosSinResumen() {
        val res = RecorteErrores.recortar(errores(50))
        assertEquals(50, res.size)
        assertEquals(errores(50), res)
        assertTrue(res.none { it.numeroLinea == 0 })
    }

    @Test
    fun cincuentaYUnErroresDevuelve50MasYUnoMas() {
        val res = RecorteErrores.recortar(errores(51))
        assertEquals(51, res.size)
        assertEquals(errores(50), res.take(50))
        assertEquals(ImportadorCsv.ErrorLinea(0, "y 1 más"), res.last())
    }

    @Test
    fun cientoVeinteErroresDevuelve50MasYSetentaMas() {
        val res = RecorteErrores.recortar(errores(120))
        assertEquals(51, res.size)
        assertEquals(errores(50), res.take(50))
        assertEquals(ImportadorCsv.ErrorLinea(0, "y 70 más"), res.last())
        // Solo hay UNA línea resumen.
        assertEquals(1, res.count { it.numeroLinea == 0 })
    }
}
