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

    @Test
    fun actualizacionEtiquetaVaciaConservaExistente() {
        val existentes = listOf(
            Regla(id = 1, patron = "400", etiqueta = "original", activa = true, creadaEn = 0, tipo = TipoRegla.PREFIJO, accion = Accion.BLOQUEAR, prioridad = 5)
        )
        val filas = listOf(
            FilaCsv(numeroLineaOriginal = 2, patron = "400", etiqueta = "", tipo = TipoRegla.PREFIJO, accion = Accion.BLOQUEAR, prioridad = 5)
        )
        val plan = PlanificadorImportacion.planificarImportacion(existentes, filas)
        assertTrue(plan.operaciones[0] is OperacionCsv.Omitir)
    }

    @Test
    fun actualizacionPrioridadVaciaConservaExistente() {
        val existentes = listOf(
            Regla(id = 1, patron = "400", etiqueta = null, activa = true, creadaEn = 0, tipo = TipoRegla.PREFIJO, accion = Accion.BLOQUEAR, prioridad = 7)
        )
        val filas = listOf(
            FilaCsv(numeroLineaOriginal = 2, patron = "400", etiqueta = null, tipo = TipoRegla.PREFIJO, accion = Accion.BLOQUEAR, prioridad = null)
        )
        val plan = PlanificadorImportacion.planificarImportacion(existentes, filas)
        assertTrue(plan.operaciones[0] is OperacionCsv.Omitir)
    }

    @Test
    fun altaPrioridadVaciaUsaPorDefecto() {
        val existentes = emptyList<Regla>()
        val plan = PlanificadorImportacion.planificarImportacion(existentes, listOf(
            FilaCsv(numeroLineaOriginal = 1, patron = "611", etiqueta = null, tipo = TipoRegla.PREFIJO, accion = Accion.BLOQUEAR, prioridad = null)
        ))
        val op = plan.operaciones[0] as OperacionCsv.Insertar
        assertEquals(0, op.prioridad)
    }

    // ---------- 1a) prioridad vacía al actualizar ----------

    @Test
    fun actualizacionCambiaAccionYPrioridadVaciaUsaPorDefectoDeLaNuevaAccion() {
        val existentes = listOf(
            Regla(id = 1, patron = "611", etiqueta = null, activa = true, creadaEn = 0, tipo = TipoRegla.PREFIJO, accion = Accion.BLOQUEAR, prioridad = 7)
        )
        val filas = listOf(
            FilaCsv(numeroLineaOriginal = 2, patron = "611", etiqueta = null, tipo = TipoRegla.PREFIJO, accion = Accion.PERMITIR, prioridad = null)
        )
        val op = PlanificadorImportacion.planificarImportacion(existentes, filas).operaciones.single()
        assertTrue(op is OperacionCsv.Actualizar)
        assertEquals(Accion.PERMITIR, (op as OperacionCsv.Actualizar).accion)
        assertEquals(100, op.prioridad)
    }

    @Test
    fun actualizacionPermitirABloquearPrioridadVaciaDaCero() {
        val existentes = listOf(
            Regla(id = 1, patron = "611", etiqueta = null, activa = true, creadaEn = 0, tipo = TipoRegla.PREFIJO, accion = Accion.PERMITIR, prioridad = 100)
        )
        val filas = listOf(
            FilaCsv(numeroLineaOriginal = 2, patron = "611", etiqueta = null, tipo = TipoRegla.PREFIJO, accion = Accion.BLOQUEAR, prioridad = null)
        )
        val op = PlanificadorImportacion.planificarImportacion(existentes, filas).operaciones.single() as OperacionCsv.Actualizar
        assertEquals(0, op.prioridad)
    }

    @Test
    fun actualizacionMismaAccionPrioridadVaciaConservaLaExistenteAunqueCambieOtroCampo() {
        val existentes = listOf(
            Regla(id = 1, patron = "611", etiqueta = "vieja", activa = true, creadaEn = 0, tipo = TipoRegla.PREFIJO, accion = Accion.PERMITIR, prioridad = 42)
        )
        val filas = listOf(
            FilaCsv(numeroLineaOriginal = 2, patron = "611", etiqueta = "nueva", tipo = TipoRegla.PREFIJO, accion = Accion.PERMITIR, prioridad = null)
        )
        val op = PlanificadorImportacion.planificarImportacion(existentes, filas).operaciones.single() as OperacionCsv.Actualizar
        // Conserva 42; NO se cambia a 100 (por defecto de PERMITIR).
        assertEquals(42, op.prioridad)
        assertEquals("nueva", op.etiqueta)
    }

    @Test
    fun sinCambiosMismaAccionPrioridadVaciaSeOmiteAunqueNoSeaLaDeDefecto() {
        // PERMITIR con prioridad 42 (no 100): con prioridad vacía y misma acción
        // la comparación usa 42, así que no hay cambios.
        val existentes = listOf(
            Regla(id = 1, patron = "611", etiqueta = null, activa = true, creadaEn = 0, tipo = TipoRegla.PREFIJO, accion = Accion.PERMITIR, prioridad = 42)
        )
        val filas = listOf(
            FilaCsv(numeroLineaOriginal = 2, patron = "611", etiqueta = null, tipo = TipoRegla.PREFIJO, accion = Accion.PERMITIR, prioridad = null)
        )
        val op = PlanificadorImportacion.planificarImportacion(existentes, filas).operaciones.single()
        assertTrue(op is OperacionCsv.Omitir)
    }

    @Test
    fun cambioDeAccionConPrioridadVaciaNoSeConsideraSinCambios() {
        // Aunque la prioridad existente (100) coincide con la de defecto de la
        // nueva acción, la acción cambia: debe actualizarse.
        val existentes = listOf(
            Regla(id = 1, patron = "611", etiqueta = null, activa = true, creadaEn = 0, tipo = TipoRegla.PREFIJO, accion = Accion.BLOQUEAR, prioridad = 100)
        )
        val filas = listOf(
            FilaCsv(numeroLineaOriginal = 2, patron = "611", etiqueta = null, tipo = TipoRegla.PREFIJO, accion = Accion.PERMITIR, prioridad = null)
        )
        val op = PlanificadorImportacion.planificarImportacion(existentes, filas).operaciones.single() as OperacionCsv.Actualizar
        assertEquals(100, op.prioridad)
    }

    // ---------- 1b) una fila inválida no "ocupa" el patrón ----------

    @Test
    fun filaInvalidaNoConvierteEnDuplicadaAUnaPosteriorValida() {
        val filas = listOf(
            // Inválida: prioridad fuera de rango.
            FilaCsv(numeroLineaOriginal = 2, patron = "611", etiqueta = null, tipo = TipoRegla.PREFIJO, accion = Accion.BLOQUEAR, prioridad = 150),
            // Válida con el mismo patrón (escrito distinto, misma normalización).
            FilaCsv(numeroLineaOriginal = 3, patron = "+34 611", etiqueta = null, tipo = TipoRegla.PREFIJO, accion = Accion.BLOQUEAR, prioridad = 0),
            // Ahora sí: repetida tras una válida -> duplicado.
            FilaCsv(numeroLineaOriginal = 4, patron = "611", etiqueta = null, tipo = TipoRegla.PREFIJO, accion = Accion.BLOQUEAR, prioridad = 0)
        )
        val ops = PlanificadorImportacion.planificarImportacion(emptyList(), filas).operaciones
        assertEquals("prioridad fuera de rango", (ops[0] as OperacionCsv.Error).motivo)
        assertTrue(ops[1] is OperacionCsv.Insertar)
        assertEquals("611", (ops[1] as OperacionCsv.Insertar).patron)
        assertEquals("duplicado en el fichero", (ops[2] as OperacionCsv.Error).motivo)
    }

    @Test
    fun filaConCaracteresInvalidosNoConvierteEnDuplicadaAUnaPosteriorValida() {
        // "611x" falla en ValidacionReglas (carácter no válido); "611" después es válida.
        val filas = listOf(
            FilaCsv(numeroLineaOriginal = 2, patron = "611x", etiqueta = null, tipo = TipoRegla.PREFIJO, accion = Accion.BLOQUEAR, prioridad = 0),
            FilaCsv(numeroLineaOriginal = 3, patron = "611", etiqueta = null, tipo = TipoRegla.PREFIJO, accion = Accion.BLOQUEAR, prioridad = 0)
        )
        val ops = PlanificadorImportacion.planificarImportacion(emptyList(), filas).operaciones
        assertTrue(ops[0] is OperacionCsv.Error)
        assertTrue(ops[1] is OperacionCsv.Insertar)
    }

    @Test
    fun actualizacionEtiquetaAusenteConservaYEtiquetaNuevaSustituye() {
        val existentes = listOf(
            Regla(id = 1, patron = "611", etiqueta = "original", activa = true, creadaEn = 0, tipo = TipoRegla.PREFIJO, accion = Accion.BLOQUEAR, prioridad = 0)
        )
        val conserva = PlanificadorImportacion.planificarImportacion(existentes, listOf(
            FilaCsv(numeroLineaOriginal = 2, patron = "611", etiqueta = null, tipo = TipoRegla.PREFIJO, accion = Accion.BLOQUEAR, prioridad = 5)
        )).operaciones.single() as OperacionCsv.Actualizar
        assertEquals("original", conserva.etiqueta)

        val sustituye = PlanificadorImportacion.planificarImportacion(existentes, listOf(
            FilaCsv(numeroLineaOriginal = 2, patron = "611", etiqueta = "  otra  ", tipo = TipoRegla.PREFIJO, accion = Accion.BLOQUEAR, prioridad = 0)
        )).operaciones.single() as OperacionCsv.Actualizar
        assertEquals("otra", sustituye.etiqueta)
    }

    @Test
    fun validacionFallaAunquePatronExisteEnBd() {
        val existentes = listOf(
            Regla(id = 1, patron = "400", etiqueta = "x", activa = true, creadaEn = 0, tipo = TipoRegla.PREFIJO, accion = Accion.BLOQUEAR, prioridad = 0)
        )
        val filas = listOf(
            FilaCsv(numeroLineaOriginal = 2, patron = "abc", etiqueta = null, tipo = TipoRegla.PREFIJO, accion = Accion.BLOQUEAR, prioridad = 0)
        )
        val plan = PlanificadorImportacion.planificarImportacion(existentes, filas)
        assertTrue(plan.operaciones[0] is OperacionCsv.Error)
    }
}
