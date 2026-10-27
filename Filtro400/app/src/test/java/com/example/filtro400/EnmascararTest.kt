package com.example.filtro400

import com.example.filtro400.util.Enmascarar
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Comprobamos que nunca se escribe el número completo en un log.
 */
class EnmascararTest {

    @Test
    fun enmascaraUnNumeroDeNueveDigitos() {
        assertEquals("400123***", Enmascarar.numero("400123456"))
    }

    @Test
    fun normalizaAntesDeEnmascarar() {
        assertEquals("400123***", Enmascarar.numero("+34 400 123 456"))
    }

    @Test
    fun numeroVacioQuedaTotalmenteOculto() {
        assertEquals("***", Enmascarar.numero(""))
    }

    @Test
    fun enmascaradoExactoSegunEspecificacion() {
        assertEquals("400123***", Enmascarar.numero("+34 400 123 456"))
        assertEquals("400123***", Enmascarar.numero("400123456"))
        assertEquals("+44791***", Enmascarar.numero("+447911123456"))
        assertEquals("12***", Enmascarar.numero("1234"))
    }
}
