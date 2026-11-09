package com.example.filtro400.util

import java.time.Instant
import java.time.ZoneId

/**
 * Cálculo de límites de día para las estadísticas. Kotlin puro (java.time,
 * disponible desde API 26; minSdk = 29) y con la ZONA INYECTADA para poder
 * probarlo en JVM con cualquier zona, incluida una con cambio de hora.
 *
 * Por qué no "ahora - 24 h": "hoy" significa "desde las 00:00 de hoy en la
 * hora local del usuario". Restar 24 h (o usar UTC) metería llamadas de ayer
 * y, en los días de cambio de hora, el día no dura 24 h (23 o 25 h).
 */
object Fechas {

    /** Milisegundos de las 00:00 locales (en [zona]) del día que contiene [instante]. */
    fun inicioDelDia(instante: Long, zona: ZoneId): Long =
        inicioDelDiaHaceDias(instante, 0, zona)

    /**
     * Milisegundos de las 00:00 locales del día que está [dias] días de
     * calendario antes del día de [instante]. Con `dias = 0` es [inicioDelDia].
     *
     * Se resta en días de CALENDARIO (`LocalDate.minusDays`), no en bloques de
     * 24 h, así que un cambio de hora en medio no desplaza el resultado.
     */
    fun inicioDelDiaHaceDias(instante: Long, dias: Int, zona: ZoneId): Long {
        require(dias >= 0) { "dias no puede ser negativo" }
        return Instant.ofEpochMilli(instante)
            .atZone(zona)
            .toLocalDate()
            .minusDays(dias.toLong())
            // atStartOfDay(zona) devuelve la primera hora válida del día aunque
            // en esa zona no exista la medianoche por un cambio de hora.
            .atStartOfDay(zona)
            .toInstant()
            .toEpochMilli()
    }
}
