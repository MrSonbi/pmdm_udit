package com.example.filtro400.registro

import com.example.filtro400.modelo.Accion
import com.example.filtro400.modelo.TipoRegla

/**
 * Fila de registro de actividad.
 */
data class RegistroLlamada(
    val id: Long = 0L,
    val fecha: Long,
    val numeroEnmascarado: String,
    val decision: Accion,
    val reglaId: Long? = null,
    val reglaPatron: String? = null,
    val reglaTipo: TipoRegla? = null,
    val reglaPrioridad: Int? = null,
    val motivo: String? = null,
    val origen: OrigenLlamada
)
