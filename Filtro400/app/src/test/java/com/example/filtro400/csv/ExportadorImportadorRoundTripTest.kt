package com.example.filtro400.csv

import com.example.filtro400.modelo.Accion
import com.example.filtro400.modelo.Regla
import com.example.filtro400.modelo.TipoRegla
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets

class ExportadorImportadorRoundTripTest {
    @Test
    fun idaYVueltaConComasComillasPuntoYComa() {
        val reglas = listOf(
            Regla(1, "400", "Llamadas, \"comerciales\"; España", false, 0, TipoRegla.PREFIJO, Accion.BLOQUEAR, 0),
            Regla(2, "611", null, true, 0, TipoRegla.NUMERO, Accion.PERMITIR, 100)
        )
        val texto = ExportadorCsv.exportar(reglas)
        val res = CsvCodec.leerConResultado(ByteArrayInputStream(texto.toByteArray(StandardCharsets.UTF_8)))
        assertEquals(2, res.filas.size)
        val plan = PlanificadorImportacion.planificarImportacion(reglas, res.filas)
        assertTrue(plan.operaciones.all { it is OperacionCsv.Omitir })
    }

    @Test
    fun csvConCabeceraFormatoAcordado() {
        val csv = "patron;tipo;accion;prioridad;activa;etiqueta\n400;PREFIJO;BLOQUEAR;0;1;Llamadas comerciales\n"
        val res = CsvCodec.leerConResultado(ByteArrayInputStream(csv.toByteArray(StandardCharsets.UTF_8)))
        assertEquals(1, res.filas.size)
        assertEquals("400", res.filas[0].patron)
        assertEquals(TipoRegla.PREFIJO, res.filas[0].tipo)
    }
}
