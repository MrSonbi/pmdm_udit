package com.example.filtro400.motor

import com.example.filtro400.ReglaBloqueo
import com.example.filtro400.modelo.Accion
import com.example.filtro400.modelo.TipoRegla

/**
 * Resultado de validar lo que el usuario escribe en el diálogo de reglas.
 *
 * Si [error] no es null, no se puede guardar. Si [requiereConfirmacion] es true,
 * el patrón es válido pero tan corto que la UI debe preguntar antes de guardarlo.
 */
data class ResultadoValidacion(
    val patron: String? = null,
    val error: String? = null,
    val requiereConfirmacion: Boolean = false,
    val tipo: TipoRegla = TipoRegla.PREFIJO,
    val accion: Accion = Accion.BLOQUEAR,
    val prioridad: Int = 0
) {
    val esValida: Boolean get() = error == null && patron != null
}

/**
 * Validación pura (sin Android) de reglas de la Fase 3.
 *
 * - Soporta [TipoRegla.PREFIJO] y [TipoRegla.NUMERO].
 * - Valida [Accion] y rango [prioridad] 0..100.
 * - Calcula la prioridad por defecto según acción si no se indica: PERMITIR -> 100, BLOQUEAR -> 0.
 * - Es importante normalizar ANTES de guardar porque el motor compara siempre
 *   contra el patrón normalizado. También es importante rechazar "+34": al
 *   quitar el prefijo de país se queda vacío.
 */
object ValidacionReglas {

    private const val MAX_DIGITOS = 15

    // Separadores habituales que el usuario puede escribir; normalizar los quita.
    private val SEPARADORES = setOf(' ', '\t', '-', '.', '(', ')')

    fun validar(
        entrada: String,
        patronesExistentes: Collection<String>,
        tipo: TipoRegla = TipoRegla.PREFIJO,
        accion: Accion = Accion.BLOQUEAR,
        prioridad: Int? = null
    ): ResultadoValidacion {
        if (prioridad != null && prioridad !in 0..100) {
            return ResultadoValidacion(error = "La prioridad debe estar entre 0 y 100.")
        }

        val recortado = entrada.trim()
        if (recortado.isEmpty()) {
            return ResultadoValidacion(error = "Escribe un prefijo o un número.")
        }

        // Rechazamos letras y símbolos raros ANTES de normalizar.
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

        // En modo NUMERO la regla debe ser un número completo (tras normalizar,
        // sin prefijo de país). Seguimos siendo permisivos con lo que ya exista,
        // pero evitamos guardar algo incoherente con el tipo elegido.
        if (tipo == TipoRegla.NUMERO && digitos == 0) {
            return ResultadoValidacion(error = "Para coincidencia exacta se necesita un número.")
        }

        val yaExiste = patronesExistentes.any { existente ->
            ReglaBloqueo.normalizar(existente) == patron
        }
        if (yaExiste) {
            return ResultadoValidacion(error = "Ya existe una regla con ese patrón.")
        }

        val prioridadCalculada = prioridad ?: when (accion) {
            Accion.BLOQUEAR -> 0
            Accion.PERMITIR -> 100
        }

        return ResultadoValidacion(
            patron = patron,
            requiereConfirmacion = tipo == TipoRegla.PREFIJO && digitos <= 2,
            tipo = tipo,
            accion = accion,
            prioridad = prioridadCalculada
        )
    }

    /**
     * Prioridad por defecto según acción. Úsalo también desde CSV/UI cuando
     * la prioridad falte o venga vacía.
     */
    fun prioridadPorDefecto(accion: Accion): Int = when (accion) {
        Accion.BLOQUEAR -> 0
        Accion.PERMITIR -> 100
    }

    private fun Char.isDigitoAscii(): Boolean = this in '0'..'9'
}

