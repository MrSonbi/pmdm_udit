package com.example.filtro400.motor

import com.example.filtro400.ReglaBloqueo
import com.example.filtro400.data.Regla

/** Qué debe hacer el filtro con la llamada. La Fase 3 añadirá la lista blanca. */
enum class Accion {
    BLOQUEAR,
    PERMITIR
}

/**
 * Resultado de evaluar un número.
 *
 * - [regla] es la regla que ha coincidido, o null si ninguna coincide.
 * - [motivo] es un texto breve para mostrar en el probador/registro.
 */
data class Decision(
    val accion: Accion,
    val regla: Regla? = null,
    val motivo: String
) {
    val bloquea: Boolean get() = accion == Accion.BLOQUEAR
}

/**
 * Motor de filtrado en Kotlin puro: no importa nada de Android, así se puede
 * probar con JUnit en el ordenador sin emulador.
 *
 * Fase 2: solo hay reglas de prefijo que bloquean. Aun así ya elegimos la regla
 * MÁS LARGA cuando coinciden varias; en la Fase 3 eso será el desempate por
 * especificidad (NUMERO > prefijo más largo).
 */
object MotorFiltrado {

    fun evaluar(numero: String, reglas: List<Regla>): Decision {
        val normalizado = ReglaBloqueo.normalizar(numero)

        // Un número oculto llega vacío. Nunca se debe bloquear a ciegas.
        if (normalizado.isEmpty()) {
            return Decision(
                accion = Accion.PERMITIR,
                regla = null,
                motivo = "Número vacío u oculto: se permite"
            )
        }

        val coincidencia = reglas
            .asSequence()
            .filter { it.activa }
            .filter { it.patron.isNotEmpty() }
            .filter { normalizado.startsWith(it.patron) }
            .maxByOrNull { it.patron.length }

        return if (coincidencia == null) {
            Decision(
                accion = Accion.PERMITIR,
                regla = null,
                motivo = "Ninguna regla activa coincide"
            )
        } else {
            Decision(
                accion = Accion.BLOQUEAR,
                regla = coincidencia,
                motivo = "Coincide con la regla «${coincidencia.patron}»"
            )
        }
    }
}
