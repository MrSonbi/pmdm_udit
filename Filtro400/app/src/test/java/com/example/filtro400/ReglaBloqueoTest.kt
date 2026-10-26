package com.example.filtro400

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/**
 * Tests de la normalización: es la base de todas las comparaciones de la app.
 */
class ReglaBloqueoTest {

    @Test
    fun normalizaNumeroEspanolConMas() {
        assertEquals("400123456", ReglaBloqueo.normalizar("+34 400 123 456"))
    }

    @Test
    fun normalizaNumeroEspanolCon0034() {
        assertEquals("400123456", ReglaBloqueo.normalizar("0034 400 123 456"))
    }

    @Test
    fun quitaEspaciosGuionesPuntosYParentesis() {
        assertEquals("400123456", ReglaBloqueo.normalizar("(400) 123-456."))
    }

    @Test
    fun ceroCeroInicialEquivaleASignoMas() {
        assertEquals(
            ReglaBloqueo.normalizar("+44 400 123 456"),
            ReglaBloqueo.normalizar("0044 400 123 456")
        )
    }

    @Test
    fun internacionalNoSeConfundeConPrefijo400() {
        val normalizado = ReglaBloqueo.normalizar("+44 400 123 456")
        assertEquals("+44400123456", normalizado)
        assertFalse(normalizado.startsWith("400"))
    }

    @Test
    fun numeroVacioSigueVacio() {
        assertEquals("", ReglaBloqueo.normalizar(""))
    }

    @Test
    fun soloPrefijoDePaisQuedaVacio() {
        assertEquals("", ReglaBloqueo.normalizar("+34"))
        assertEquals("", ReglaBloqueo.normalizar("0034"))
    }
}
