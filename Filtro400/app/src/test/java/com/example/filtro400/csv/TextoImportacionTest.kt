package com.example.filtro400.csv

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TextoImportacionTest {

    @Test
    fun resumenConPluralesYErroneas() {
        val r = ImportadorCsv.ResultadoImportacion(
            anyadidas = 3,
            actualizadas = 1,
            omitidas = 0,
            erroneas = 2
        )
        assertEquals(
            "Importación completada: 3 añadidas, 1 actualizada, 0 omitidas (sin cambios), 2 erróneas.",
            TextoImportacion.resumen(r)
        )
    }

    @Test
    fun resumenConSingulares() {
        val r = ImportadorCsv.ResultadoImportacion(anyadidas = 1, actualizadas = 0, omitidas = 1, erroneas = 1)
        assertEquals(
            "Importación completada: 1 añadida, 0 actualizadas, 1 omitida (sin cambios), 1 errónea.",
            TextoImportacion.resumen(r)
        )
    }

    @Test
    fun falloDiceQueNoSeHaCambiadoNada() {
        val r = ImportadorCsv.ResultadoImportacion(fallo = true, mensaje = "El fichero supera el límite de 1 MB.")
        val texto = TextoImportacion.resumen(r)
        assertEquals(
            "Importación cancelada. El fichero supera el límite de 1 MB. No se ha cambiado ninguna regla.",
            texto
        )
        assertTrue(texto.contains("No se ha cambiado"))
    }

    @Test
    fun lineaConNumeroYLineaResumen() {
        assertEquals(
            "Línea 7: patrón vacío",
            TextoImportacion.linea(ImportadorCsv.ErrorLinea(7, "patrón vacío"))
        )
        // La línea "y N más" (numeroLinea = 0) se muestra sin prefijo.
        assertEquals("y 70 más", TextoImportacion.linea(ImportadorCsv.ErrorLinea(0, "y 70 más")))
    }

    @Test
    fun recorteMasTextoDaCincuentaLineasYResumen() {
        val todos = (1..120).map { ImportadorCsv.ErrorLinea(it, "mal") }
        val recortados = RecorteErrores.recortar(todos)
        val textos = recortados.map { TextoImportacion.linea(it) }
        assertEquals(51, textos.size)
        assertEquals("Línea 1: mal", textos.first())
        assertEquals("Línea 50: mal", textos[49])
        assertEquals("y 70 más", textos.last())
    }
}
