package com.example.filtro400

import com.example.filtro400.util.Fechas
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime

/**
 * "Hoy" = desde las 00:00 de la zona local, no "últimas 24 h" ni UTC.
 */
class FechasTest {

    private val madrid = ZoneId.of("Europe/Madrid")
    private val hora = 60L * 60L * 1000L

    /** Instante (ms) de una fecha y hora LOCAL en [zona]. */
    private fun ms(anyo: Int, mes: Int, dia: Int, h: Int, min: Int, zona: ZoneId): Long =
        ZonedDateTime.of(anyo, mes, dia, h, min, 0, 0, zona).toInstant().toEpochMilli()

    @Test
    fun inicioDelDiaEnDiaNormal() {
        val mediodia = ms(2026, 11, 9, 12, 0, madrid)
        assertEquals(ms(2026, 11, 9, 0, 0, madrid), Fechas.inicioDelDia(mediodia, madrid))
        // Día sin cambio de hora: del inicio a las 12:00 hay 12 h.
        assertEquals(12 * hora, mediodia - Fechas.inicioDelDia(mediodia, madrid))
    }

    @Test
    fun justoALasDoceEsElPropioInstanteYUnMsAntesEsAyer() {
        val medianoche = ms(2026, 11, 9, 0, 0, madrid)
        assertEquals(medianoche, Fechas.inicioDelDia(medianoche, madrid))
        assertEquals(ms(2026, 11, 8, 0, 0, madrid), Fechas.inicioDelDia(medianoche - 1, madrid))
    }

    @Test
    fun noEsUtcNiUltimas24Horas() {
        // 00:30 del 10/11 en Madrid = 23:30 UTC del 9/11.
        val instante = ms(2026, 11, 10, 0, 30, madrid)
        val inicioMadrid = Fechas.inicioDelDia(instante, madrid)
        val inicioUtc = Fechas.inicioDelDia(instante, ZoneOffset.UTC)

        assertEquals(ms(2026, 11, 10, 0, 0, madrid), inicioMadrid)
        assertEquals(ms(2026, 11, 9, 0, 0, ZoneOffset.UTC), inicioUtc)
        assertNotEquals(inicioMadrid, inicioUtc)
        // Tampoco es "ahora - 24 h": desde las 00:00 solo han pasado 30 min.
        assertEquals(30L * 60L * 1000L, instante - inicioMadrid)
    }

    @Test
    fun diaConCambioAHoraDeVeranoDura23Horas() {
        // 29/03/2026 en Madrid: a las 02:00 (CET, +01) se pasa a las 03:00 (CEST, +02).
        val mediodia = ms(2026, 3, 29, 12, 0, madrid)
        val inicio = Fechas.inicioDelDia(mediodia, madrid)
        // 00:00 CET = 23:00 UTC del 28/03.
        assertEquals(ms(2026, 3, 28, 23, 0, ZoneOffset.UTC), inicio)
        // De las 00:00 a las 12:00 de ese día solo hay 11 h reales.
        assertEquals(11 * hora, mediodia - inicio)
        // El día siguiente empieza 23 h después.
        val inicioSiguiente = Fechas.inicioDelDia(ms(2026, 3, 30, 12, 0, madrid), madrid)
        assertEquals(23 * hora, inicioSiguiente - inicio)
    }

    @Test
    fun diaConCambioAHoraDeInviernoDura25Horas() {
        // 25/10/2026 en Madrid: a las 03:00 (CEST, +02) se vuelve a las 02:00 (CET, +01).
        val mediodia = ms(2026, 10, 25, 12, 0, madrid)
        val inicio = Fechas.inicioDelDia(mediodia, madrid)
        // 00:00 CEST = 22:00 UTC del 24/10.
        assertEquals(ms(2026, 10, 24, 22, 0, ZoneOffset.UTC), inicio)
        assertEquals(13 * hora, mediodia - inicio)
        val inicioSiguiente = Fechas.inicioDelDia(ms(2026, 10, 26, 12, 0, madrid), madrid)
        assertEquals(25 * hora, inicioSiguiente - inicio)
    }

    @Test
    fun sieteDiasDeCalendarioAtraviesanElCambioDeHora() {
        // Hoy 31/03/2026 (CEST). "Últimos 7 días" = hoy y los 6 anteriores:
        // desde las 00:00 del 25/03, que todavía era CET (+01).
        val ahora = ms(2026, 3, 31, 12, 0, madrid)
        val desde = Fechas.inicioDelDiaHaceDias(ahora, 6, madrid)
        assertEquals(ms(2026, 3, 25, 0, 0, madrid), desde)
        assertEquals(ms(2026, 3, 24, 23, 0, ZoneOffset.UTC), desde)
        // Restar 6 × 24 h desde las 00:00 de hoy daría las 23:00 locales del 24/03
        // (una hora de más, porque el 29/03 duró 23 h): incorrecto.
        val ingenuo = Fechas.inicioDelDia(ahora, madrid) - 6 * 24 * hora
        assertEquals(hora, desde - ingenuo)
    }

    @Test
    fun ceroDiasEsInicioDelDia() {
        val ahora = ms(2026, 11, 9, 18, 45, madrid)
        assertEquals(Fechas.inicioDelDia(ahora, madrid), Fechas.inicioDelDiaHaceDias(ahora, 0, madrid))
    }
}
