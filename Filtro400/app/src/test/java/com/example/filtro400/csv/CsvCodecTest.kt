package com.example.filtro400.csv

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets

class CsvCodecTest {

    @Test
    fun bomYSeparadorPuntoYComa() {
        val csv = "\uFEFF400;PREFIJO;BLOQUEAR;0;1;Comerciales\n"
        val res = CsvCodec.leerConResultado(ByteArrayInputStream(csv.toByteArray(StandardCharsets.UTF_8)))
        assertEquals(1, res.filas.size)
        assertEquals(0, res.errores.size)
    }

    @Test
    fun separadorComa() {
        val csv = "400,PREFIJO,BLOQUEAR,0,1,Comerciales\n"
        val res = CsvCodec.leerConResultado(ByteArrayInputStream(csv.toByteArray(StandardCharsets.UTF_8)))
        assertEquals(1, res.filas.size)
        assertEquals(0, res.errores.size)
    }

    @Test
    fun comillasYComasDentroDeEtiqueta() {
        val csv = "\"400\";\"PREFIJO\";\"BLOQUEAR\";\"0\";\"1\";\"Etiqueta, con coma\"\n"
        val res = CsvCodec.leerConResultado(ByteArrayInputStream(csv.toByteArray(StandardCharsets.UTF_8)))
        assertEquals(1, res.filas.size)
        assertEquals("Etiqueta, con coma", res.filas[0].etiqueta)
        assertEquals(0, res.errores.size)
    }

    @Test
    fun filaConMenosColumnasGeneraErrorSinAbortar() {
        val csv = "400;etiqueta\n611;PREFIJO;BLOQUEAR;0;1;eti\n"
        val res = CsvCodec.leerConResultado(ByteArrayInputStream(csv.toByteArray(StandardCharsets.UTF_8)))
        assertEquals(1, res.filas.size)
        assertEquals(1, res.errores.size)
        assertEquals(1, res.errores[0].numeroLineaOriginal)
    }

    @Test
    fun tipoOAccionDesconocidosGeneraErrorSinAbortar() {
        val csv = "400;PREFIJO;RARO;0;1;eti\n611;PREFIJO;BLOQUEAR;0;1;eti\n"
        val res = CsvCodec.leerConResultado(ByteArrayInputStream(csv.toByteArray(StandardCharsets.UTF_8)))
        assertEquals(1, res.filas.size)
        assertEquals(1, res.errores.size)
    }

    @Test
    fun prioridadVaciaAcepta() {
        val csv = "400;NUMERO;PERMITIR;;;\n"
        val res = CsvCodec.leerConResultado(ByteArrayInputStream(csv.toByteArray(StandardCharsets.UTF_8)))
        assertEquals(1, res.filas.size)
        assertEquals(null, res.filas[0].prioridad)
    }

    @Test
    fun prioridad101GeneraErrorSinAbortar() {
        val csv = "400;PREFIJO;BLOQUEAR;101;1;eti\n611;PREFIJO;BLOQUEAR;0;1;eti\n"
        val res = CsvCodec.leerConResultado(ByteArrayInputStream(csv.toByteArray(StandardCharsets.UTF_8)))
        assertEquals(1, res.filas.size)
        assertEquals(1, res.errores.size)
    }

    @Test
    fun lineaEnBlancoIntermediaConNumeracionCorrecta() {
        val csv = "400;PREFIJO;BLOQUEAR;0;1;eti\n\n611;NUMERO;PERMITIR;;;\n"
        val res = CsvCodec.leerConResultado(ByteArrayInputStream(csv.toByteArray(StandardCharsets.UTF_8)))
        assertEquals(2, res.filas.size)
        assertEquals(1, res.filas[0].numeroLineaOriginal)
        assertEquals(3, res.filas[1].numeroLineaOriginal)
    }

    @Test
    fun lineaDemasiadoLargaGeneraErrorSinAbortar() {
        val larga = "a".repeat(1001)
        val csv = "$larga;PREFIJO;BLOQUEAR;0;1;eti\n611;PREFIJO;BLOQUEAR;0;1;eti\n"
        val res = CsvCodec.leerConResultado(ByteArrayInputStream(csv.toByteArray(StandardCharsets.UTF_8)))
        assertEquals(1, res.filas.size)
        assertEquals(1, res.errores.size)
    }

    @Test
    fun extremoAExtremoParseoMasPlan() {
        val csv = "400;PREFIJO;BLOQUEAR;0;1;eti\n" + // valida
                "badtype;PREFIJO;RARO;0;1;eti\n" + // tipo desconocido
                "611;PREFIJO;BLOQUEAR;101;1;eti\n" + // prioridad 101
                "\n" + // linea en blanco
                "622;NUMERO;PERMITIR;;;\n" + // valida
                "400;PREFIJO;BLOQUEAR;0;1;eti2\n" + // patron repetido en fichero
                "40;PREFIJO;BLOQUEAR;0;1;eti\n" // prefijo corto
        val resParseo = CsvCodec.leerConResultado(ByteArrayInputStream(csv.toByteArray(StandardCharsets.UTF_8)))
        assertTrue(resParseo.filas.size >= 2)
        assertTrue(resParseo.errores.size >= 2)
        val plan = PlanificadorImportacion.planificarImportacion(emptyList(), resParseo.filas)
        val erroresPlan = plan.operaciones.filterIsInstance<OperacionCsv.Error>().map { it.numeroLineaOriginal to it.motivo }
        val todos = (resParseo.errores.map { it.numeroLineaOriginal to it.motivo } + erroresPlan).sortedBy { it.first }
        assertTrue(todos.isNotEmpty())
    }
}
