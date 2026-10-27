package com.example.filtro400.modelo

import com.example.filtro400.ReglaBloqueo

/**
 * Regla de filtrado guardada en SQLite (Fase 3).
 *
 * [patron] siempre está normalizado con [ReglaBloqueo.normalizar()].
 * La prioridad rige el desempate cuando coinciden varias reglas: menor valor
 * o mayor, lo decide el motor, pero los valores por defecto son claros:
 * BLOQUEAR -> 0, PERMITIR -> 100. El usuario puede editarlos.
 */
data class Regla(
    val id: Long = 0L,
    val patron: String,
    val etiqueta: String? = null,
    val activa: Boolean = true,
    val creadaEn: Long = System.currentTimeMillis(),
    val tipo: TipoRegla = TipoRegla.PREFIJO,
    val accion: Accion = Accion.BLOQUEAR,
    val prioridad: Int = 0
) {
    init {
        require(prioridad in 0..100) {
            "La prioridad debe estar entre 0 y 100, se recibió $prioridad"
        }
    }
}
