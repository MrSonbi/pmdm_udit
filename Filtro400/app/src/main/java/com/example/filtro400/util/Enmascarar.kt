package com.example.filtro400.util

import com.example.filtro400.ReglaBloqueo

/**
 * Oculta parte de un número antes de escribirlo en Logcat o en el registro.
 *
 * Regla GENERAL (sin casos especiales por valor concreto):
 *  1. Se normaliza con [ReglaBloqueo.normalizar] (la única normalización de la app).
 *  2. Si queda vacío (número oculto) -> "(oculto)".
 *  3. Si no, se muestra solo un prefijo seguido de "***":
 *     - longitud <= 3 -> 1 carácter visible;
 *     - longitud <= 6 -> 2 caracteres visibles;
 *     - resto         -> min(6, longitud - 3) caracteres visibles.
 *
 * Ejemplos: "400123456" -> "400123***", "1234" -> "12***", "+447911123456" -> "+44791***".
 * Nunca se debe guardar ni mostrar el número completo en logs.
 */
object Enmascarar {

    /** Texto que se devuelve cuando no hay número que mostrar. */
    const val OCULTO = "(oculto)"

    private const val SUFIJO = "***"

    fun numero(numero: String): String {
        val normalizado = ReglaBloqueo.normalizar(numero)
        if (normalizado.isEmpty()) return OCULTO
        return normalizado.take(caracteresVisibles(normalizado.length)) + SUFIJO
    }

    /** Cuántos caracteres del principio se pueden enseñar según la longitud. */
    private fun caracteresVisibles(longitud: Int): Int = when {
        longitud <= 3 -> 1
        longitud <= 6 -> 2
        else -> minOf(6, longitud - 3)
    }
}
