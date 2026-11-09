package com.example.filtro400.ui

import com.example.filtro400.modelo.Accion
import com.example.filtro400.modelo.Regla
import com.example.filtro400.modelo.TipoRegla

/*
 * Textos legibles para los enums del modelo. En pantalla no se enseñan los
 * nombres internos (PREFIJO, BLOQUEAR...), sino palabras en español normal.
 *
 * Son funciones PURAS (sin Android ni Compose): se prueban con JUnit (TextosTest).
 */

internal fun textoTipo(tipo: TipoRegla): String = when (tipo) {
    TipoRegla.PREFIJO -> "Prefijo"
    TipoRegla.NUMERO -> "Número exacto"
}

internal fun textoAccion(accion: Accion): String = when (accion) {
    Accion.BLOQUEAR -> "Bloquear"
    Accion.PERMITIR -> "Permitir (lista blanca)"
}

/**
 * Frase llana que explica una regla, para la tarjeta de la pestaña Reglas.
 * Empieza siempre por el verbo ("Bloquea" / "Permite siempre"), así la acción
 * se entiende por el texto y no solo por el color.
 *
 *  - PREFIJO + BLOQUEAR -> "Bloquea llamadas que empiezan por 400"
 *  - NUMERO  + BLOQUEAR -> "Bloquea el número 400555123"
 *  - PREFIJO + PERMITIR -> "Permite siempre llamadas que empiezan por 400"
 *  - NUMERO  + PERMITIR -> "Permite siempre el número 400555123"
 */
internal fun fraseRegla(tipo: TipoRegla, accion: Accion, patron: String): String {
    val verbo = when (accion) {
        Accion.BLOQUEAR -> "Bloquea"
        Accion.PERMITIR -> "Permite siempre"
    }
    val objeto = when (tipo) {
        TipoRegla.PREFIJO -> "llamadas que empiezan por $patron"
        TipoRegla.NUMERO -> "el número $patron"
    }
    return "$verbo $objeto"
}

/** Atajo para una regla ya guardada. */
internal fun fraseRegla(regla: Regla): String = fraseRegla(regla.tipo, regla.accion, regla.patron)

/** Estado escrito de una regla (nunca se comunica solo con el interruptor). */
internal fun textoEstadoRegla(activa: Boolean): String = if (activa) "Activa" else "Pausada"

/**
 * Filtro del selector de la pestaña Reglas: Todas / Bloqueos / Lista blanca.
 * [acepta] es puro y se prueba en TextosTest.
 */
internal enum class FiltroReglas(val texto: String) {
    TODAS("Todas"),
    BLOQUEOS("Bloqueos"),
    LISTA_BLANCA("Lista blanca");

    fun acepta(regla: Regla): Boolean = when (this) {
        TODAS -> true
        BLOQUEOS -> regla.accion == Accion.BLOQUEAR
        LISTA_BLANCA -> regla.accion == Accion.PERMITIR
    }
}

/** Frase amable cuando la lista (o el filtro elegido) se queda vacía. */
internal fun textoReglasVacias(filtro: FiltroReglas, hayReglas: Boolean): String = when {
    !hayReglas ->
        "Aún no tienes reglas. Mientras tanto, todas las llamadas entran con normalidad."
    filtro == FiltroReglas.BLOQUEOS ->
        "No tienes reglas que bloqueen llamadas."
    filtro == FiltroReglas.LISTA_BLANCA ->
        "No tienes números en la lista blanca."
    else ->
        "No hay reglas que mostrar."
}
