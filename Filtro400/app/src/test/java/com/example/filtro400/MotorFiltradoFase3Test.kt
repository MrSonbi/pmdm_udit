package com.example.filtro400

import com.example.filtro400.modelo.Accion
import com.example.filtro400.modelo.Regla
import com.example.filtro400.modelo.TipoRegla
import com.example.filtro400.motor.MotorFiltrado
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Tests del motor para los requisitos de Fase 3.
 *
 * Desempate: prioridad -> NUMERO exacto -> patrón más largo -> PERMITIR.
 */
class MotorFiltradoFase3Test {

    private fun regla(
        patron: String,
        tipo: TipoRegla = TipoRegla.PREFIJO,
        accion: Accion = Accion.BLOQUEAR,
        prioridad: Int = 0,
        activa: Boolean = true,
        id: Long = 1L
    ): Regla = Regla(
        id = id,
        patron = patron,
        activa = activa,
        tipo = tipo,
        accion = accion,
        prioridad = prioridad
    )

    @Test
    fun ganaPrioridadSobreLongitud() {
        // Ambas coinciden, pero la de mayor prioridad debe imponerse.
        val decision = MotorFiltrado.evaluar(
            "400123456",
            listOf(
                regla(patron = "400", prioridad = 0, id = 1L),
                regla(patron = "4", prioridad = 10, id = 2L)
            )
        )
        assertEquals(Accion.BLOQUEAR, decision.accion)
        assertEquals("4", decision.regla?.patron)
    }

    @Test
    fun numeroExactoGanaAPrefijo() {
        val decision = MotorFiltrado.evaluar(
            "400123456",
            listOf(
                regla(patron = "400", tipo = TipoRegla.PREFIJO, prioridad = 50, id = 1L),
                regla(patron = "400123456", tipo = TipoRegla.NUMERO, prioridad = 50, id = 2L)
            )
        )
        assertEquals(Accion.BLOQUEAR, decision.accion)
        assertEquals("400123456", decision.regla?.patron)
    }

    @Test
    fun ganaPatronMasLargoConMismaPrioridadYSinNumeroExacto() {
        val decision = MotorFiltrado.evaluar(
            "400123456",
            listOf(
                regla(patron = "4001", prioridad = 5, id = 1L),
                regla(patron = "40012", prioridad = 5, id = 2L)
            )
        )
        assertEquals(Accion.BLOQUEAR, decision.accion)
        assertEquals("40012", decision.regla?.patron)
    }

    @Test
    fun permiteGanaEnEmpateAbsoluto() {
        // Mismo patrón, mismo tipo, misma prioridad: PERMITIR (lista blanca)
        // debe imponerse sobre BLOQUEAR.
        val decision = MotorFiltrado.evaluar(
            "400123456",
            listOf(
                regla(patron = "400", accion = Accion.BLOQUEAR, prioridad = 3, id = 1L),
                regla(patron = "400", accion = Accion.PERMITIR, prioridad = 3, id = 2L)
            )
        )
        assertEquals(Accion.PERMITIR, decision.accion)
        assertEquals("400", decision.regla?.patron)
    }

    @Test
    fun listaBlancaConMayorPrioridadPermiteAunqueHayaBloqueo() {
        val decision = MotorFiltrado.evaluar(
            "900123456",
            listOf(
                regla(patron = "9", accion = Accion.BLOQUEAR, prioridad = 0, id = 1L),
                regla(patron = "900123456", tipo = TipoRegla.NUMERO, accion = Accion.PERMITIR, prioridad = 100, id = 2L)
            )
        )
        assertEquals(Accion.PERMITIR, decision.accion)
        assertNull(decision.regla?.let { if (it.accion == Accion.BLOQUEAR) it else null })
    }

    @Test
    fun reglaNumeroExactaNoCoincideSiEsDiferente() {
        val decision = MotorFiltrado.evaluar(
            "400123456",
            listOf(regla(patron = "400654321", tipo = TipoRegla.NUMERO))
        )
        assertEquals(Accion.PERMITIR, decision.accion)
        assertNull(decision.regla)
    }
}
