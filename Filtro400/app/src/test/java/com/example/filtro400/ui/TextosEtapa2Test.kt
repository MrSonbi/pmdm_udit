package com.example.filtro400.ui

import com.example.filtro400.csv.ImportadorCsv
import com.example.filtro400.modelo.Accion
import com.example.filtro400.modelo.Regla
import com.example.filtro400.modelo.TipoRegla
import com.example.filtro400.motor.MotorFiltrado
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Textos del rediseño (etapa 2): la frase del simulador (4 casos), el plural
 * de "llamada", la regla aplicada del registro y el resumen de importación.
 *
 * Las decisiones salen del MISMO motor puro que usa el servicio, así la frase
 * se prueba con decisiones reales y no inventadas.
 */
class TextosEtapa2Test {

    private val bloqueo400 = Regla(id = 1, patron = "400", accion = Accion.BLOQUEAR, prioridad = 0)
    private val blanca = Regla(
        id = 2,
        patron = "400555123",
        tipo = TipoRegla.NUMERO,
        accion = Accion.PERMITIR,
        prioridad = 100,
        etiqueta = "Banco"
    )
    private val reglas = listOf(bloqueo400, blanca)

    private fun frase(numero: String) = fraseSimulacion(numero, MotorFiltrado.evaluar(numero, reglas))

    @Test
    fun simulacion_bloqueada_por_una_regla() {
        assertEquals("La llamada se bloquearía por la regla 400.", frase("+34 400 111 222"))
    }

    @Test
    fun simulacion_permitida_por_lista_blanca() {
        assertEquals(
            "La llamada entraría: está en tu lista blanca, regla 400555123 (Banco).",
            frase("400 555 123")
        )
    }

    @Test
    fun simulacion_permitida_sin_coincidencias() {
        assertEquals("La llamada entraría: no coincide con ninguna de tus reglas.", frase("612345678"))
    }

    @Test
    fun simulacion_numero_vacio_u_oculto() {
        val esperado = "No hay número (vacío u oculto). La llamada entraría: el filtro nunca bloquea a ciegas."
        assertEquals(esperado, frase(""))
        assertEquals(esperado, frase("  - "))
    }

    @Test
    fun plural_de_llamadas() {
        assertEquals("0 llamadas", textoLlamadas(0))
        assertEquals("1 llamada", textoLlamadas(1))
        assertEquals("3 llamadas", textoLlamadas(3))
    }

    @Test
    fun regla_aplicada_del_registro() {
        assertEquals("Regla 400", textoReglaAplicada("400"))
        assertEquals("Ninguna regla", textoReglaAplicada(null))
    }

    @Test
    fun lineas_del_resumen_de_importacion() {
        val r = ImportadorCsv.ResultadoImportacion(anyadidas = 3, actualizadas = 1, omitidas = 0, erroneas = 2)
        assertEquals(
            listOf("Añadidas: 3", "Actualizadas: 1", "Omitidas (sin cambios): 0", "Erróneas: 2"),
            lineasResumenImportacion(r)
        )
    }

    @Test
    fun selector_de_reglas_en_una_palabra() {
        assertEquals(listOf("Todas", "Bloqueos", "Permitidas"), FiltroReglas.entries.map { it.texto })
    }
}
