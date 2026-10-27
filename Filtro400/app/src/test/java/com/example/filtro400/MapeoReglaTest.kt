package com.example.filtro400

import com.example.filtro400.data.MapeoRegla
import com.example.filtro400.modelo.Accion
import com.example.filtro400.modelo.TipoRegla
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Test JVM puro del mapeo de reglas: debe ser robusto ante valores
 * corruptos y nunca lanzar excepciones.
 */
class MapeoReglaTest {

    @Test
    fun mapeaFilaValida() {
        val regla = MapeoRegla.leerRegla(
            id = 1L,
            patron = "400",
            etiqueta = "test",
            activa = true,
            creadaEn = 1000L,
            tipoNombre = "PREFIJO",
            accionNombre = "BLOQUEAR",
            prioridadValor = 0
        )
        assertNotNull(regla)
        assertEquals(TipoRegla.PREFIJO, regla!!.tipo)
        assertEquals(Accion.BLOQUEAR, regla.accion)
        assertEquals(0, regla.prioridad)
    }

    @Test
    fun omiteFilaConPrioridadFueraDeRango() {
        assertNull(
            MapeoRegla.leerRegla(
                id = 1L,
                patron = "400",
                etiqueta = null,
                activa = true,
                creadaEn = 1000L,
                tipoNombre = "PREFIJO",
                accionNombre = "BLOQUEAR",
                prioridadValor = 101
            )
        )
        assertNull(
            MapeoRegla.leerRegla(
                id = 2L,
                patron = "400",
                etiqueta = null,
                activa = true,
                creadaEn = 1000L,
                tipoNombre = "PREFIJO",
                accionNombre = "BLOQUEAR",
                prioridadValor = -1
            )
        )
        assertNull(
            MapeoRegla.leerRegla(
                id = 3L,
                patron = "400",
                etiqueta = null,
                activa = true,
                creadaEn = 1000L,
                tipoNombre = "PREFIJO",
                accionNombre = "BLOQUEAR",
                prioridadValor = null
            )
        )
    }

    @Test
    fun omiteFilaConTipoOAccionDesconocidos() {
        assertNull(
            MapeoRegla.leerRegla(
                id = 1L,
                patron = "400",
                etiqueta = null,
                activa = true,
                creadaEn = 1000L,
                tipoNombre = "RARA",
                accionNombre = "BLOQUEAR",
                prioridadValor = 0
            )
        )
        assertNull(
            MapeoRegla.leerRegla(
                id = 2L,
                patron = "400",
                etiqueta = null,
                activa = true,
                creadaEn = 1000L,
                tipoNombre = "PREFIJO",
                accionNombre = "IGNORAR",
                prioridadValor = 0
            )
        )
        assertNull(
            MapeoRegla.leerRegla(
                id = 3L,
                patron = "400",
                etiqueta = null,
                activa = true,
                creadaEn = 1000L,
                tipoNombre = null,
                accionNombre = "BLOQUEAR",
                prioridadValor = 0
            )
        )
    }
}
