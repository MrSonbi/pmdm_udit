package com.example.filtro400.datos

import android.content.Context

/**
 * Ajustes compartidos en SharedPreferences.
 * El ajuste "guardar registro" está activo por defecto.
 */
object Preferencias {

    private const val NOMBRE = "filtro400_prefs"
    private const val CLAVE_GUARDAR_REGISTRO = "guardar_registro"

    fun guardarRegistroActivado(contexto: Context): Boolean {
        val prefs = contexto.applicationContext.getSharedPreferences(NOMBRE, Context.MODE_PRIVATE)
        return prefs.getBoolean(CLAVE_GUARDAR_REGISTRO, true)
    }

    fun setGuardarRegistro(contexto: Context, activar: Boolean) {
        val prefs = contexto.applicationContext.getSharedPreferences(NOMBRE, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(CLAVE_GUARDAR_REGISTRO, activar).apply()
    }
}
