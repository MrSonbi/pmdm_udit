package com.example.filtro400

import com.example.filtro400.modelo.Accion
import com.example.filtro400.modelo.TipoRegla
import com.example.filtro400.motor.ValidacionReglas
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests de validación para los nuevos campos de Fase 3.
 */
class ValidacionReglasFase3Test {

    @Test
    fun bloquearPorDefectoTienePrioridadCero() {
        val resultado = ValidacionReglas.validar("911", emptyList(), tipo = TipoRegla.PREFIJO, accion = Accion.BLOQUEAR)
        assertTrue(resultado.esValida)
        assertEquals(0, resultado.prioridad)
        assertEquals(Accion.BLOQUEAR, resultado.accion)
    }

    @Test
    fun permitirPorDefectoTienePrioridadCien() {
        val resultado = ValidacionReglas.validar("611", emptyList(), tipo = TipoRegla.PREFIJO, accion = Accion.PERMITIR)
        assertTrue(resultado.esValida)
        assertEquals(100, resultado.prioridad)
        assertEquals(Accion.PERMITIR, resultado.accion)
    }

    @Test
    fun permiteFijarPrioridadManual() {
        val resultado = ValidacionReglas.validar("900", emptyList(), accion = Accion.BLOQUEAR, prioridad = 50)
        assertTrue(resultado.esValida)
        assertEquals(50, resultado.prioridad)
    }

    @Test
    fun rechazaPrioridadFueraDeRango() {
        assertNotNull(ValidacionReglas.validar("900", emptyList(), prioridad = -1).error)
        assertNotNull(ValidacionReglas.validar("900", emptyList(), prioridad = 101).error)
    }

    @Test
    fun validaReglaNumeroExacto() {
        val resultado = ValidacionReglas.validar("612345678", emptyList(), tipo = TipoRegla.NUMERO, accion = Accion.BLOQUEAR)
        assertTrue(resultado.esValida)
        assertEquals("612345678", resultado.patron)
        assertEquals(TipoRegla.NUMERO, resultado.tipo)
        // En modo exacto no forzamos la confirmación por longitud del prefijo.
        assertFalse(resultado.requiereConfirmacion)
    }

    @Test
    fun prioridadPorDefectoHelperCoincide() {
        assertEquals(0, ValidacionReglas.prioridadPorDefecto(Accion.BLOQUEAR))
        assertEquals(100, ValidacionReglas.prioridadPorDefecto(Accion.PERMITIR))
    }
}
