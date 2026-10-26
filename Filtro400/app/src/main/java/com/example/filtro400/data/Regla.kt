package com.example.filtro400.data

/**
 * Una regla de filtrado guardada en SQLite.
 *
 * En la Fase 2 todas las reglas son prefijos que BLOQUEAN. La Fase 3 añadirá
 * tipo (prefijo/número), acción (bloquear/permitir) y prioridad; por eso el
 * resto de la app ya trabaja con objetos [Regla] y no con filas sueltas de SQL.
 *
 * [patron] siempre está normalizado con `ReglaBloqueo.normalizar()`.
 */
data class Regla(
    val id: Long = 0L,
    val patron: String,
    val etiqueta: String? = null,
    val activa: Boolean = true,
    val creadaEn: Long = System.currentTimeMillis()
)
