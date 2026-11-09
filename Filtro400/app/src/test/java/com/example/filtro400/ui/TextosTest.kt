package com.example.filtro400.ui

import com.example.filtro400.modelo.Accion
import com.example.filtro400.modelo.Regla
import com.example.filtro400.modelo.TipoRegla
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Frases llanas de la pestaña Reglas: las 4 combinaciones tipo x acción. */
class TextosTest {

    @Test
    fun prefijo_bloquear() {
        assertEquals(
            "Bloquea llamadas que empiezan por 400",
            fraseRegla(TipoRegla.PREFIJO, Accion.BLOQUEAR, "400")
        )
    }

    @Test
    fun numero_bloquear() {
        assertEquals(
            "Bloquea el número 400555123",
            fraseRegla(TipoRegla.NUMERO, Accion.BLOQUEAR, "400555123")
        )
    }

    @Test
    fun prefijo_permitir() {
        assertEquals(
            "Permite siempre llamadas que empiezan por 400",
            fraseRegla(TipoRegla.PREFIJO, Accion.PERMITIR, "400")
        )
    }

    @Test
    fun numero_permitir() {
        assertEquals(
            "Permite siempre el número 400555123",
            fraseRegla(TipoRegla.NUMERO, Accion.PERMITIR, "400555123")
        )
    }

    @Test
    fun frase_de_una_regla_usa_su_tipo_accion_y_patron() {
        val regla = Regla(id = 1, patron = "+44123", tipo = TipoRegla.PREFIJO, accion = Accion.PERMITIR, prioridad = 100)
        assertEquals("Permite siempre llamadas que empiezan por +44123", fraseRegla(regla))
    }

    @Test
    fun estado_se_escribe_con_texto() {
        assertEquals("Activa", textoEstadoRegla(true))
        assertEquals("Pausada", textoEstadoRegla(false))
    }

    @Test
    fun filtro_de_reglas_por_accion() {
        val bloqueo = Regla(id = 1, patron = "400", accion = Accion.BLOQUEAR)
        val blanca = Regla(id = 2, patron = "400555123", tipo = TipoRegla.NUMERO, accion = Accion.PERMITIR, prioridad = 100)
        assertTrue(FiltroReglas.TODAS.acepta(bloqueo) && FiltroReglas.TODAS.acepta(blanca))
        assertTrue(FiltroReglas.BLOQUEOS.acepta(bloqueo))
        assertFalse(FiltroReglas.BLOQUEOS.acepta(blanca))
        assertTrue(FiltroReglas.LISTA_BLANCA.acepta(blanca))
        assertFalse(FiltroReglas.LISTA_BLANCA.acepta(bloqueo))
    }
}
