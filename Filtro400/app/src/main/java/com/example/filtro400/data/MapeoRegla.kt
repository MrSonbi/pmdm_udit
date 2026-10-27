package com.example.filtro400.data

import com.example.filtro400.modelo.Accion
import com.example.filtro400.modelo.Regla
import com.example.filtro400.modelo.TipoRegla
import com.example.filtro400.motor.ValidacionReglas

/**
 * Conversión pura de datos de BD a [Regla].
 * Se extrae para poder testearla sin Cursor ni Android.
 */
object MapeoRegla {

    fun leerRegla(
        id: Long,
        patron: String,
        etiqueta: String?,
        activa: Boolean,
        creadaEn: Long,
        tipoNombre: String?,
        accionNombre: String?,
        prioridadValor: Int?
    ): Regla? {
        val tipo = tipoNombre?.let { nombre ->
            runCatching { TipoRegla.valueOf(nombre) }.getOrNull()
        } ?: return null

        val accion = accionNombre?.let { nombre ->
            runCatching { Accion.valueOf(nombre) }.getOrNull()
        } ?: return null

        val prioridad = prioridadValor ?: return null
        if (prioridad !in 0..100) {
            return null
        }

        return Regla(
            id = id,
            patron = patron,
            etiqueta = etiqueta,
            activa = activa,
            creadaEn = creadaEn,
            tipo = tipo,
            accion = accion,
            prioridad = prioridad
        )
    }
}
