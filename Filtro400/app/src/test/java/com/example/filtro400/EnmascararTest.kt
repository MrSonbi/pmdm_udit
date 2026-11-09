package com.example.filtro400

import com.example.filtro400.util.Enmascarar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Comprobamos que nunca se escribe el número completo en un log o en el registro.
 */
class EnmascararTest {

    @Test
    fun enmascaradoExactoSegunEspecificacion() {
        assertEquals("400123***", Enmascarar.numero("+34 400 123 456"))
        assertEquals("400123***", Enmascarar.numero("400123456"))
        assertEquals("+44791***", Enmascarar.numero("+447911123456"))
        assertEquals("12***", Enmascarar.numero("1234"))
    }

    @Test
    fun numeroVacioDevuelveOculto() {
        assertEquals("(oculto)", Enmascarar.numero(""))
        // Solo separadores: tras normalizar no queda nada.
        assertEquals("(oculto)", Enmascarar.numero(" - "))
    }

    @Test
    fun tramosDeLongitud() {
        assertEquals("1***", Enmascarar.numero("1"))
        assertEquals("1***", Enmascarar.numero("123"))
        assertEquals("12***", Enmascarar.numero("12345"))
        assertEquals("12***", Enmascarar.numero("123456"))
        assertEquals("1234***", Enmascarar.numero("1234567"))
    }

    /**
     * Propiedades que deben cumplirse para CUALQUIER longitud de 1 a 15 dígitos.
     * Se usan dígitos que empiezan por 9 para que la normalización no toque
     * nada (no empiezan por "00" ni "0034"), aunque igualmente se compara
     * contra el resultado de normalizar.
     */
    @Test
    fun propiedadesParaNumerosDe1A15Digitos() {
        val base = "987654321098765"
        for (longitud in 1..15) {
            val original = base.take(longitud)
            val normalizado = ReglaBloqueo.normalizar(original)
            val resultado = Enmascarar.numero(original)

            assertNotEquals("longitud $longitud: devuelve el original", original, resultado)
            assertTrue("longitud $longitud: no termina en ***", resultado.endsWith("***"))

            val visible = resultado.removeSuffix("***")
            assertTrue(
                "longitud $longitud: ${visible.length} caracteres visibles (> 6)",
                visible.length <= 6
            )
            assertTrue(
                "longitud $longitud: '$visible' no es prefijo de '$normalizado'",
                normalizado.startsWith(visible)
            )
        }
    }
}
