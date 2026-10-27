package com.example.filtro400.modelo

/**
 * Tipo de coincidencia que se aplica al patrón normalizado.
 *
 * - [PREFIJO]: cualquier número que empiece por el patrón.
 * - [NUMERO]: coincidencia exacta con el patrón.
 */
enum class TipoRegla {
    PREFIJO,
    NUMERO
}
