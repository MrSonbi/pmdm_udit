package com.example.filtro400.util

import com.example.filtro400.ReglaBloqueo

/**
 * Oculta parte de un número antes de escribir en Logcat o en el futuro registro.
 *
 * Ejemplo: "400123456" -> "400123***".
 * Nunca se debe guardar ni mostrar el número completo en logs.
 */
object Enmascarar {

    fun numero(numero: String): String {
        val normalizado = ReglaBloqueo.normalizar(numero)
        if (normalizado.length <= 3) return "***"

        // Dejamos como máximo 6 caracteres visibles y ocultamos el resto.
        val visibles = minOf(6, normalizado.length - 3)
        return normalizado.take(visibles) + "***"
    }
}
