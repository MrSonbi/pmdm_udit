package com.example.filtro400.csv

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ResumenPlanTest {
    @Test
    fun resumenPuro() {
        val plan = PlanImportacion(
            listOf(
                OperacionCsv.Insertar(1, "400", null, com.example.filtro400.modelo.TipoRegla.PREFIJO, com.example.filtro400.modelo.Accion.BLOQUEAR, 0, true),
                OperacionCsv.Omitir(2),
                OperacionCsv.Error(3, "mal")
            )
        )
        val r = ResumidorPlan.resumenDe(plan)
        assertEquals(1, r.anyadidas)
        assertEquals(0, r.actualizadas)
        assertEquals(1, r.omitidas)
        assertEquals(1, r.erroneas)
    }

    @Test
    fun planVacio() {
        val plan = PlanImportacion(emptyList())
        val r = ResumidorPlan.resumenDe(plan)
        assertEquals(0, r.anyadidas)
        assertEquals(0, r.actualizadas)
        assertEquals(0, r.omitidas)
        assertEquals(0, r.erroneas)
        assertTrue(r.errores.isEmpty())
    }

    @Test
    fun mezclaInsertarActualizarOmitirError() {
        val plan = PlanImportacion(
            listOf(
                OperacionCsv.Insertar(1, "400", null, com.example.filtro400.modelo.TipoRegla.PREFIJO, com.example.filtro400.modelo.Accion.BLOQUEAR, 0, true),
                OperacionCsv.Insertar(2, "611", null, com.example.filtro400.modelo.TipoRegla.PREFIJO, com.example.filtro400.modelo.Accion.BLOQUEAR, 0, true),
                OperacionCsv.Actualizar(3, 10L, "622", "x", com.example.filtro400.modelo.TipoRegla.PREFIJO, com.example.filtro400.modelo.Accion.PERMITIR, 100, true),
                OperacionCsv.Omitir(4),
                OperacionCsv.Error(5, "mal"),
                OperacionCsv.Error(6, "otro")
            )
        )
        val r = ResumidorPlan.resumenDe(plan)
        assertEquals(2, r.anyadidas)
        assertEquals(1, r.actualizadas)
        assertEquals(1, r.omitidas)
        assertEquals(2, r.erroneas)
        assertEquals(2, r.errores.size)
    }

    @Test
    fun errorNoCuentaComoOmitida() {
        val plan = PlanImportacion(
            listOf(
                OperacionCsv.Error(1, "inválido")
            )
        )
        val r = ResumidorPlan.resumenDe(plan)
        assertEquals(0, r.anyadidas)
        assertEquals(0, r.actualizadas)
        assertEquals(0, r.omitidas)
        assertEquals(1, r.erroneas)
    }
}
