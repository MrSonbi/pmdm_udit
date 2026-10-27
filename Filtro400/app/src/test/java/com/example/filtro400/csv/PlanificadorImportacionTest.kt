package com.example.filtro400.csv

import com.example.filtro400.modelo.Accion
import com.example.filtro400.modelo.Regla
import com.example.filtro400.modelo.TipoRegla
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlanificadorImportacionTest {

    @Test
    fun patronExistenteActualizar() {
        val existentes = listOf(
            Regla(id = 1, patron = "400", etiqueta = null, activa = true, creadaEn = 0, tipo = TipoRegla.PREFIJO, accion = Accion.BLOQUEAR, prioridad = 0)
        )
        val filas = listOf(
            FilaCsv(numeroLineaOriginal = 2, patron = "400", etiqueta = "x", tipo = TipoRegla.PREFIJO, accion = Accion.BLOQUEAR, prioridad = 0)
        )
        val plan = PlanificadorImportacion.planificarImportacion(existentes, filas)
        assertTrue(plan.operaciones[0] is OperacionCsv.Actualizar)
    }

    @Test
    fun filaRepetidaEnElFicheroSegundaEsError() {
        val existentes = emptyList<Regla>()
        val filas = listOf(
            FilaCsv(numeroLineaOriginal = 1, patron = "400", etiqueta = null, tipo = TipoRegla.PREFIJO, accion = Accion.BLOQUEAR, prioridad = 0),
            FilaCsv(numeroLineaOriginal = 2, patron = "400", etiqueta = null, tipo = TipoRegla.PREFIJO, accion = Accion.BLOQUEAR, prioridad = 0)
        )
        val plan = PlanificadorImportacion.planificarImportacion(existentes, filas)
        assertTrue(plan.operaciones[0] is OperacionCsv.Insertar)
        assertTrue(plan.operaciones[1] is OperacionCsv.Error)
        assertEquals(2, (plan.operaciones[1] as OperacionCsv.Error).numeroLineaOriginal)
    }

    @Test
    fun filaInvalidaErrorConNumeroDeLinea() {
        val existentes = emptyList<Regla>()
        val filas = listOf(
            FilaCsv(numeroLineaOriginal = 5, patron = "abc", etiqueta = null, tipo = TipoRegla.PREFIJO, accion = Accion.BLOQUEAR, prioridad = 0)
        )
        val plan = PlanificadorImportacion.planificarImportacion(existentes, filas)
        assertTrue(plan.operaciones[0] is OperacionCsv.Error)
        assertEquals(5, (plan.operaciones[0] as OperacionCsv.Error).numeroLineaOriginal)
    }

    @Test
    fun activaCeroSeRespeta() {
        val existentes = emptyList<Regla>()
        val filas = listOf(
            FilaCsv(numeroLineaOriginal = 1, patron = "611", etiqueta = null, tipo = TipoRegla.PREFIJO, accion = Accion.BLOQUEAR, prioridad = 0, activa = false)
        )
        val plan = PlanificadorImportacion.planificarImportacion(existentes, filas)
        val op = plan.operaciones[0] as OperacionCsv.Insertar
        assertEquals(false, op.activa)
    }

    @Test
    fun activaVacia() {
        val existentes = emptyList<Regla>()
        val filas = listOf(
            FilaCsv(numeroLineaOriginal = 1, patron = "611", etiqueta = null, tipo = TipoRegla.PREFIJO, accion = Accion.BLOQUEAR, prioridad = 0, activa = null)
        )
        val plan = PlanificadorImportacion.planificarImportacion(existentes, filas)
        val op = plan.operaciones[0] as OperacionCsv.Insertar
        assertEquals(true, op.activa)
    }

    @Test
    fun prefijoCortoError() {
        val existentes = emptyList<Regla>()
        val filas = listOf(
            FilaCsv(numeroLineaOriginal = 3, patron = "40", etiqueta = null, tipo = TipoRegla.PREFIJO, accion = Accion.BLOQUEAR, prioridad = 0)
        )
        val plan = PlanificadorImportacion.planificarImportacion(existentes, filas)
        assertTrue(plan.operaciones[0] is OperacionCsv.Error)
        assertEquals("prefijo demasiado corto para importar", (plan.operaciones[0] as OperacionCsv.Error).motivo)
    }

    @Test
    fun prioridadVaciaPermitirDa100YBloquearDa0() {
        val existentes = emptyList<Regla>()
        val plan1 = PlanificadorImportacion.planificarImportacion(existentes, listOf(
            FilaCsv(numeroLineaOriginal = 1, patron = "611", etiqueta = null, tipo = TipoRegla.PREFIJO, accion = Accion.PERMITIR, prioridad = null)
        ))
        assertEquals(100, (plan1.operaciones[0] as OperacionCsv.Insertar).prioridad)
        val plan2 = PlanificadorImportacion.planificarImportacion(existentes, listOf(
            FilaCsv(numeroLineaOriginal = 2, patron = "622", etiqueta = null, tipo = TipoRegla.PREFIJO, accion = Accion.BLOQUEAR, prioridad = null)
        ))
        assertEquals(0, (plan2.operaciones[0] as OperacionCsv.Insertar).prioridad)
    }

    @Test
    fun filaIdenticaAExistenteOmitir() {
        val existentes = listOf(
            Regla(id = 1, patron = "400", etiqueta = null, activa = true, creadaEn = 0, tipo = TipoRegla.PREFIJO, accion = Accion.BLOQUEAR, prioridad = 0)
        )
        val filas = listOf(
            FilaCsv(numeroLineaOriginal = 1, patron = "400", etiqueta = null, tipo = TipoRegla.PREFIJO, accion = Accion.BLOQUEAR, prioridad = 0, activa = null)
        )
        val plan = PlanificadorImportacion.planificarImportacion(existentes, filas)
        assertTrue(plan.operaciones[0] is OperacionCsv.Omitir)
    }

    @Test
    fun nuevaInsertar() {
        val existentes = emptyList<Regla>()
        val filas = listOf(
            FilaCsv(numeroLineaOriginal = 1, patron = "611", etiqueta = null, tipo = TipoRegla.PREFIJO, accion = Accion.BLOQUEAR, prioridad = 0)
        )
        val plan = PlanificadorImportacion.planificarImportacion(existentes, filas)
        assertTrue(plan.operaciones[0] is OperacionCsv.Insertar)
    }
}
