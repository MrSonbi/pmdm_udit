package com.example.filtro400

import com.example.filtro400.motor.ValidacionReglas
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests de la validación que se ejecuta antes de guardar una regla.
 */
class ValidacionReglasTest {

    @Test
    fun validaYNormalizaUnNumeroConSeparadores() {
        val resultado = ValidacionReglas.validar("+34 400 123 456", emptyList())

        assertTrue(resultado.esValida)
        assertEquals("400123456", resultado.patron)
        assertNull(resultado.error)
        assertFalse(resultado.requiereConfirmacion)
    }

    @Test
    fun aceptaPuntosGuionesYParentesis() {
        val resultado = ValidacionReglas.validar("(400) 123-456.", emptyList())

        assertTrue(resultado.esValida)
        assertEquals("400123456", resultado.patron)
    }

    @Test
    fun rechazaEntradaVacia() {
        assertNotNull(ValidacionReglas.validar("   ", emptyList()).error)
    }

    @Test
    fun rechazaSoloPrefijoDePaisPorqueQuedaVacio() {
        assertNotNull(ValidacionReglas.validar("+34", emptyList()).error)
        assertNotNull(ValidacionReglas.validar("0034", emptyList()).error)
    }

    @Test
    fun rechazaLetrasEnLaEntradaCruda() {
        val resultado = ValidacionReglas.validar("400abc", emptyList())

        assertNotNull(resultado.error)
        assertNull(resultado.patron)
        assertTrue(resultado.error!!.contains("Carácter no válido"))
    }

    @Test
    fun rechazaSignoMasQueNoEstaAlPrincipio() {
        assertNotNull(ValidacionReglas.validar("400+123", emptyList()).error)
    }

    @Test
    fun rechazaDuplicadosTrasNormalizar() {
        val resultado = ValidacionReglas.validar("+34 400", listOf("400"))

        assertNotNull(resultado.error)
        assertTrue(resultado.error!!.contains("Ya existe"))
    }

    @Test
    fun detectaDuplicadoEntre0044YMas44() {
        assertNotNull(ValidacionReglas.validar("0044", listOf("+44")).error)
    }

    @Test
    fun rechazaMasDeQuinceDigitos() {
        assertNotNull(ValidacionReglas.validar("1234567890123456", emptyList()).error)
    }

    @Test
    fun aceptaQuinceDigitos() {
        val resultado = ValidacionReglas.validar("123456789012345", emptyList())

        assertTrue(resultado.esValida)
    }

    @Test
    fun pideConfirmacionConDosDigitosOMenos() {
        assertTrue(ValidacionReglas.validar("44", emptyList()).requiereConfirmacion)
        assertTrue(ValidacionReglas.validar("4", emptyList()).requiereConfirmacion)
    }

    @Test
    fun noPideConfirmacionCon400() {
        assertFalse(ValidacionReglas.validar("400", emptyList()).requiereConfirmacion)
    }
}
