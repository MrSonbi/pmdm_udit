package com.example.filtro400.motor

import com.example.filtro400.ReglaBloqueo

/**
 * Resultado de validar lo que el usuario escribe en el diálogo de reglas.
 *
 * Si [error] no es null, no se puede guardar. Si [requiereConfirmacion] es true,
 * el patrón es válido pero tan corto que la UI debe preguntar antes de guardarlo.
 */
data class ResultadoValidacion(
    val patron: String? = null,
    val error: String? = null,
    val requiereConfirmacion: Boolean = false
) {
    val esValida: Boolean get() = error == null && patron != null
}

/**
 * Validación pura (sin Android) de un prefijo antes de guardarlo.
 *
 * Es importante normalizar ANTES de guardar porque el motor compara siempre
 * contra el patrón normalizado. También es importante rechazar "+34": al
 * quitar el prefijo de país se queda vacío, y `startsWith("")` bloquearía
 * absolutamente todas las llamadas.
 */
object ValidacionReglas {

    private const val MAX_DIGITOS = 15

    // Separadores habituales que el usuario puede escribir; normalizar los quita.
    private val SEPARADORES = setOf(' ', '\t', '-', '.', '(', ')')

    fun validar(entrada: String, patronesExistentes: Collection<String>): ResultadoValidacion {
        val recortado = entrada.trim()
        if (recortado.isEmpty()) {
            return ResultadoValidacion(error = "Escribe un prefijo o un número.")
        }

        // Rechazamos letras y símbolos raros ANTES de normalizar. Si no,
        // "400abc" se convertiría en "400" sin avisar al usuario.
        val caracterInvalido = recortado.firstOrNull { caracter ->
            !caracter.isDigitoAscii() && caracter != '+' && caracter !in SEPARADORES
        }
        if (caracterInvalido != null) {
            return ResultadoValidacion(
                error = "Carácter no válido: '$caracterInvalido'. " +
                    "Usa números, +, espacios, guiones, puntos o paréntesis."
            )
        }

        val patron = ReglaBloqueo.normalizar(recortado)
        val digitos = patron.count { it in '0'..'9' }

        if (digitos == 0) {
            return ResultadoValidacion(
                error = "El patrón no puede quedar vacío. Recuerda que +34 se elimina del número."
            )
        }

        val posicionPlus = patron.indexOf('+')
        if (posicionPlus > 0 || patron.count { it == '+' } > 1) {
            return ResultadoValidacion(error = "El signo + solo puede aparecer al principio.")
        }

        if (digitos > MAX_DIGITOS) {
            return ResultadoValidacion(error = "Un número no puede tener más de $MAX_DIGITOS dígitos.")
        }

        val yaExiste = patronesExistentes.any { existente ->
            ReglaBloqueo.normalizar(existente) == patron
        }
        if (yaExiste) {
            return ResultadoValidacion(error = "Ya existe una regla con ese patrón.")
        }

        return ResultadoValidacion(
            patron = patron,
            // 2 dígitos o menos bloquearía muchísimas llamadas: pedimos confirmación.
            requiereConfirmacion = digitos <= 2
        )
    }

    private fun Char.isDigitoAscii(): Boolean = this in '0'..'9'
}
