package com.example.filtro400.motor

import com.example.filtro400.ReglaBloqueo
import com.example.filtro400.modelo.Accion
import com.example.filtro400.modelo.Regla
import com.example.filtro400.modelo.TipoRegla

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
 * Desempate (obligatorio en Fase 3):
 * 1) mayor prioridad
 * 2) coincidencia EXACTA de número ([TipoRegla.NUMERO]) antes que prefijo
 * 3) patrón más largo
 * 4) en caso de empate absoluto, gana [Accion.PERMITIR] (lista blanca)
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

        val candidatas = reglas
            .asSequence()
            .filter { it.activa }
            .filter { it.patron.isNotEmpty() }
            .filter { regla ->
                when (regla.tipo) {
                    TipoRegla.NUMERO -> normalizado == regla.patron
                    TipoRegla.PREFIJO -> normalizado.startsWith(regla.patron)
                }
            }
            .toList()

        if (candidatas.isEmpty()) {
            return Decision(
                accion = Accion.PERMITIR,
                regla = null,
                motivo = "Ninguna regla activa coincide"
            )
        }

        val elegida = candidatas.sortedWith(
            compareByDescending<Regla> { it.prioridad }
                .thenByDescending { it.tipo == TipoRegla.NUMERO }
                .thenByDescending { it.patron.length }
                .thenByDescending { it.accion == Accion.PERMITIR }
        ).first()

        return Decision(
            accion = elegida.accion,
            regla = elegida,
            motivo = when (elegida.accion) {
                Accion.BLOQUEAR -> "Bloqueado por regla «${elegida.patron}»"
                Accion.PERMITIR -> "Permitido por regla «${elegida.patron}»"
            }
        )
    }
}

