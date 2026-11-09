package com.example.filtro400.ui

import com.example.filtro400.ReglaBloqueo
import com.example.filtro400.csv.ImportadorCsv
import com.example.filtro400.modelo.Accion
import com.example.filtro400.modelo.Regla
import com.example.filtro400.modelo.TipoRegla
import com.example.filtro400.motor.Decision

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
 * Filtro del selector de la pestaña Reglas: Todas / Bloqueos / Permitidas.
 * (LISTA_BLANCA conserva su nombre interno; en pantalla dice "Permitidas" para
 * que quepa en una línea, y el subtítulo explica que es la lista blanca.)
 * [acepta] es puro y se prueba en TextosTest.
 */
internal enum class FiltroReglas(val texto: String) {
    TODAS("Todas"),
    BLOQUEOS("Bloqueos"),
    LISTA_BLANCA("Permitidas");

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

/**
 * Frase llana con el resultado del simulador ("Probar un número").
 * Cuatro casos (probados en TextosEtapa2Test):
 *  - número vacío u oculto: se permite siempre (nunca se bloquea a ciegas);
 *  - bloqueado por una regla -> "La llamada se bloquearía por la regla 400.";
 *  - permitido por una regla de lista blanca;
 *  - permitido porque no coincide ninguna regla.
 * Si la regla tiene nombre (etiqueta), se añade entre paréntesis.
 */
internal fun fraseSimulacion(numero: String, decision: Decision): String {
    // La misma normalización que el motor: un número oculto llega vacío.
    if (ReglaBloqueo.normalizar(numero).isEmpty()) {
        return "No hay número (vacío u oculto). La llamada entraría: el filtro nunca bloquea a ciegas."
    }
    val regla = decision.regla
        ?: return "La llamada entraría: no coincide con ninguna de tus reglas."
    val nombre = regla.etiqueta?.trim()?.takeIf { it.isNotEmpty() }?.let { " ($it)" }.orEmpty()
    return if (decision.bloquea) {
        "La llamada se bloquearía por la regla ${regla.patron}$nombre."
    } else {
        "La llamada entraría: está en tu lista blanca, regla ${regla.patron}$nombre."
    }
}

/** "1 llamada", "3 llamadas", "0 llamadas": plural correcto. */
internal fun textoLlamadas(n: Int): String = if (n == 1) "1 llamada" else "$n llamadas"

/** Regla aplicada en una fila del registro: "Regla 400" o "Ninguna regla". */
internal fun textoReglaAplicada(patron: String?): String =
    patron?.takeIf { it.isNotBlank() }?.let { "Regla $it" } ?: "Ninguna regla"

/**
 * Líneas del diálogo de resultado de la importación, una por cifra. Con el
 * formato "Etiqueta: N" no hay problemas de singular/plural.
 */
internal fun lineasResumenImportacion(r: ImportadorCsv.ResultadoImportacion): List<String> = listOf(
    "Añadidas: ${r.anyadidas}",
    "Actualizadas: ${r.actualizadas}",
    "Omitidas (sin cambios): ${r.omitidas}",
    "Erróneas: ${r.erroneas}"
)
