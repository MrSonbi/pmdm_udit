package com.example.filtro400.modelo

/**
 * Qué debe hacer el filtro con la llamada.
 *
 * - [BLOQUEAR]: deniega la llamada (regla por defecto de la Fase 2).
 * - [PERMITIR]: lista blanca. Gracias a la prioridad, puede ganar aunque
 *   exista una regla de bloqueo que también coincida.
 */
enum class Accion {
    BLOQUEAR,
    PERMITIR
}
