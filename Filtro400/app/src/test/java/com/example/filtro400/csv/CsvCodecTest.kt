package com.example.filtro400.csv

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets

class CsvCodecTest {

    @Test
    fun bomYSeparadorPuntoYComa() {
        val csv = "\uFEFF400;Comerciales;PREFIJO;BLOQUEAR;0\n"
        val res = CsvCodec.leerConResultado(ByteArrayInputStream(csv.toByteArray(StandardCharsets.UTF_8)))
        assertEquals(1, res.filas.size)
        assertEquals(0, res.errores.size)
    }

    @Test
    fun separadorComa() {
        val csv = "400,Comerciales,PREFIJO,BLOQUEAR,0\n"
        val res = CsvCodec.leerConResultado(ByteArrayInputStream(csv.toByteArray(StandardCharsets.UTF_8)))
        assertEquals(1, res.filas.size)
        assertEquals(0, res.errores.size)
    }

    @Test
    fun comillasYComasDentroDeEtiqueta() {
        val csv = "\"400\",\"Etiqueta, con coma\",PREFIJO,BLOQUEAR,0\n"
        val res = CsvCodec.leerConResultado(ByteArrayInputStream(csv.toByteArray(StandardCharsets.UTF_8)))
        assertEquals(1, res.filas.size)
        assertEquals("Etiqueta, con coma", res.filas[0].etiqueta)
        assertEquals(0, res.errores.size)
    }

    @Test
    fun filaConMenosColumnasGeneraErrorSinAbortar() {
        val csv = "400,etiqueta\n611,eti,PREFIJO,BLOQUEAR,0\n"
        val res = CsvCodec.leerConResultado(ByteArrayInputStream(csv.toByteArray(StandardCharsets.UTF_8)))
        assertEquals(1, res.filas.size)
        assertEquals(1, res.errores.size)
        assertEquals(1, res.errores[0].numeroLineaOriginal)
    }

    @Test
    fun tipoOAccionDesconocidosGeneraErrorSinAbortar() {
        val csv = "400,eti,PREFIJO,RARO,0\n611,eti,PREFIJO,BLOQUEAR,0\n"
        val res = CsvCodec.leerConResultado(ByteArrayInputStream(csv.toByteArray(StandardCharsets.UTF_8)))
        assertEquals(1, res.filas.size)
        assertEquals(1, res.errores.size)
    }

    @Test
    fun prioridadVaciaAcepta() {
        val csv = "400,eti,NUMERO,PERMITIR,\n"
        val res = CsvCodec.leerConResultado(ByteArrayInputStream(csv.toByteArray(StandardCharsets.UTF_8)))
        assertEquals(1, res.filas.size)
        assertEquals(null, res.filas[0].prioridad)
    }

    @Test
    fun prioridad101GeneraErrorSinAbortar() {
        val csv = "400,eti,PREFIJO,BLOQUEAR,101\n611,eti,PREFIJO,BLOQUEAR,0\n"
        val res = CsvCodec.leerConResultado(ByteArrayInputStream(csv.toByteArray(StandardCharsets.UTF_8)))
        assertEquals(1, res.filas.size)
        assertEquals(1, res.errores.size)
    }

    @Test
    fun lineaEnBlancoIntermediaConNumeracionCorrecta() {
        val csv = "400,eti,PREFIJO,BLOQUEAR,0\n\n611,eti2,NUMERO,PERMITIR,\n"
        val res = CsvCodec.leerConResultado(ByteArrayInputStream(csv.toByteArray(StandardCharsets.UTF_8)))
        assertEquals(2, res.filas.size)
        assertEquals(1, res.filas[0].numeroLineaOriginal)
        assertEquals(3, res.filas[1].numeroLineaOriginal)
    }

    @Test
    fun lineaDemasiadoLargaGeneraErrorSinAbortar() {
        val larga = "a".repeat(1001)
        val csv = "$larga,eti,PREFIJO,BLOQUEAR,0\n611,eti,PREFIJO,BLOQUEAR,0\n"
        val res = CsvCodec.leerConResultado(ByteArrayInputStream(csv.toByteArray(StandardCharsets.UTF_8)))
        assertEquals(1, res.filas.size)
        assertEquals(1, res.errores.size)
    }

    @Test
    fun extremoAExtremoParseoMasPlan() {
        val csv = "400,eti,PREFIJO,BLOQUEAR,0\n" + // valida
                "badtype,eti,PREFIJO,RARO,0\n" + // tipo desconocido
                "611,eti,PREFIJO,BLOQUEAR,101\n" + // prioridad 101
                "\n" + // linea en blanco
                "622,eti,NUMERO,PERMITIR,\n" + // valida
                "400,eti2,PREFIJO,BLOQUEAR,0\n" + // patron repetido en fichero
                "40,eti,PREFIJO,BLOQUEAR,0\n" // prefijo corto
        val resParseo = CsvCodec.leerConResultado(ByteArrayInputStream(csv.toByteArray(StandardCharsets.UTF_8)))
        assertTrue(resParseo.filas.size >= 2) // al menos las válidas
        assertTrue(resParseo.errores.size >= 2)
        val plan = PlanificadorImportacion.planificarImportacion(emptyList(), resParseo.filas)
        // fusionar errores
        val erroresPlan = plan.operaciones.filterIsInstance<OperacionCsv.Error>().map { it.numeroLineaOriginal to it.motivo }
        val todos = (resParseo.errores.map { it.numeroLineaOriginal to it.motivo } + erroresPlan).sortedBy { it.first }
        assertTrue(todos.isNotEmpty())
    }
}
