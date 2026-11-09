package com.example.filtro400.ui

import com.example.filtro400.ui.tema.PaletaNeu
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Contraste WCAG de la paleta neumórfica. Todo texto debe llegar a 4.5:1 (AA)
 * sobre el fondo en el que se pinta.
 */
class PaletaNeuTest {

    private val minimo = 4.5

    private fun exigir(nombre: String, texto: Long, fondo: Long) {
        val c = PaletaNeu.contraste(texto, fondo)
        assertTrue("$nombre: contraste %.2f < $minimo".format(c), c >= minimo)
    }

    @Test
    fun formula_wcag_da_los_extremos_conocidos() {
        assertEquals(21.0, PaletaNeu.contraste(0xFF000000, 0xFFFFFFFF), 0.01)
        assertEquals(1.0, PaletaNeu.contraste(0xFF3B5BA9, 0xFF3B5BA9), 0.0001)
    }

    @Test
    fun contraste_es_simetrico() {
        assertEquals(
            PaletaNeu.contraste(PaletaNeu.TEXTO, PaletaNeu.FONDO),
            PaletaNeu.contraste(PaletaNeu.FONDO, PaletaNeu.TEXTO),
            0.0
        )
    }

    @Test
    fun pares_pedidos_llegan_a_aa() {
        exigir("texto/fondo", PaletaNeu.TEXTO, PaletaNeu.FONDO)
        exigir("texto secundario/fondo", PaletaNeu.TEXTO_SECUNDARIO, PaletaNeu.FONDO)
        exigir("acento/fondo", PaletaNeu.ACENTO, PaletaNeu.FONDO)
        exigir("bloqueo/fondo", PaletaNeu.BLOQUEO, PaletaNeu.FONDO)
        exigir("permitir/fondo", PaletaNeu.PERMITIR, PaletaNeu.FONDO)
        exigir("blanco/acento", PaletaNeu.BLANCO, PaletaNeu.ACENTO)
    }

    @Test
    fun texto_en_piezas_hundidas_llega_a_aa() {
        // Opción activa del selector, pestaña activa y texto de los campos.
        exigir("acento/fondo hundido", PaletaNeu.ACENTO, PaletaNeu.FONDO_HUNDIDO)
        exigir("texto/fondo hundido", PaletaNeu.TEXTO, PaletaNeu.FONDO_HUNDIDO)
        exigir("texto secundario/fondo hundido", PaletaNeu.TEXTO_SECUNDARIO, PaletaNeu.FONDO_HUNDIDO)
    }

    @Test
    fun superficies_de_las_pestanas_no_redisenadas_llegan_a_aa() {
        for ((nombre, fondo) in listOf(
            "superficie" to PaletaNeu.SUPERFICIE,
            "simulada" to PaletaNeu.SIMULADA
        )) {
            exigir("texto/$nombre", PaletaNeu.TEXTO, fondo)
            exigir("texto secundario/$nombre", PaletaNeu.TEXTO_SECUNDARIO, fondo)
            exigir("acento/$nombre", PaletaNeu.ACENTO, fondo)
            exigir("bloqueo/$nombre", PaletaNeu.BLOQUEO, fondo)
            exigir("permitir/$nombre", PaletaNeu.PERMITIR, fondo)
        }
        exigir("texto/seleccion", PaletaNeu.TEXTO, PaletaNeu.SELECCION)
    }

    @Test
    fun oscurecer_hasta_no_toca_un_color_que_ya_cumple() {
        assertEquals(
            PaletaNeu.ACENTO,
            PaletaNeu.oscurecerHasta(PaletaNeu.ACENTO, PaletaNeu.FONDO)
        )
    }

    @Test
    fun oscurecer_hasta_corrige_un_color_que_no_cumple() {
        val claro = 0xFF8FA3C8 // gris azulado claro: no llega sobre el fondo
        assertTrue(PaletaNeu.contraste(claro, PaletaNeu.FONDO) < minimo)
        val corregido = PaletaNeu.oscurecerHasta(claro, PaletaNeu.FONDO)
        assertTrue(PaletaNeu.contraste(corregido, PaletaNeu.FONDO) >= minimo)
        // Conserva el alfa.
        assertEquals(0xFFL, (corregido shr 24) and 0xFF)
    }
}
