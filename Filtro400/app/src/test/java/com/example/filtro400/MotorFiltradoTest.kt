package com.example.filtro400

import com.example.filtro400.data.Regla
import com.example.filtro400.motor.Accion
import com.example.filtro400.motor.MotorFiltrado
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests del motor puro. No necesitan Android ni emulador.
 */
class MotorFiltradoTest {

    private fun regla(
        patron: String,
        activa: Boolean = true,
        id: Long = 1L
    ): Regla = Regla(id = id, patron = patron, activa = activa)

    @Test
    fun reglaActivaBloquea() {
        val decision = MotorFiltrado.evaluar("+34 400 123 456", listOf(regla("400")))

        assertEquals(Accion.BLOQUEAR, decision.accion)
        assertTrue(decision.bloquea)
        assertEquals("400", decision.regla?.patron)
        assertTrue(decision.motivo.contains("400"))
    }

    @Test
    fun reglaInactivaNoBloquea() {
        val decision = MotorFiltrado.evaluar(
            "400123456",
            listOf(regla("400", activa = false))
        )

        assertEquals(Accion.PERMITIR, decision.accion)
        assertNull(decision.regla)
    }

    @Test
    fun sinReglasPermite() {
        val decision = MotorFiltrado.evaluar("400123456", emptyList())

        assertEquals(Accion.PERMITIR, decision.accion)
        assertNull(decision.regla)
    }

    @Test
    fun numeroVacioUOcultoPermite() {
        val reglas = listOf(regla("400"))

        assertEquals(Accion.PERMITIR, MotorFiltrado.evaluar("", reglas).accion)
        assertEquals(Accion.PERMITIR, MotorFiltrado.evaluar("   ", reglas).accion)
    }

    @Test
    fun numeroInternacionalNoCoincideCon400() {
        val decision = MotorFiltrado.evaluar(
            "+44 400 123 456",
            listOf(regla("400"))
        )

        assertEquals(Accion.PERMITIR, decision.accion)
        assertNull(decision.regla)
    }

    @Test
    fun ganaLaReglaMasEspecifica() {
        val decision = MotorFiltrado.evaluar(
            "400123456",
            listOf(
                regla("4", id = 1L),
                regla("400", id = 2L)
            )
        )

        assertEquals(Accion.BLOQUEAR, decision.accion)
        assertEquals("400", decision.regla?.patron)
    }

    @Test
    fun patronVacioNoBloqueaTodasLasLlamadas() {
        val decision = MotorFiltrado.evaluar(
            "612345678",
            listOf(regla(""))
        )

        assertEquals(Accion.PERMITIR, decision.accion)
        assertNull(decision.regla)
    }
}
